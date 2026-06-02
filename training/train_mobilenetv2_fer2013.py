import argparse
from pathlib import Path

import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.model_selection import train_test_split
from sklearn.utils.class_weight import compute_class_weight


APP_LABELS = ["HAPPY", "SAD", "ANGRY", "SURPRISED"]

FER2013_ID_TO_LABEL = {
    0: "ANGRY",
    1: "DISGUST",
    2: "FEAR",
    3: "HAPPY",
    4: "SAD",
    5: "SURPRISED",
    6: "NEUTRAL",
}

FOLDER_LABEL_TO_APP_LABEL = {
    "angry": "ANGRY",
    "disgust": "DISGUST",
    "disgusted": "DISGUST",
    "fear": "FEAR",
    "happy": "HAPPY",
    "neutral": "NEUTRAL",
    "sad": "SAD",
    "surprise": "SURPRISED",
    "surprised": "SURPRISED",
}

SUPPORTED_IMAGE_EXTENSIONS = {".bmp", ".gif", ".jpeg", ".jpg", ".png"}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Train a four-class FER2013 emotion model and export a TFLite model."
    )
    input_group = parser.add_mutually_exclusive_group(required=True)
    input_group.add_argument("--csv", type=Path, help="Path to Kaggle fer2013.csv.")
    input_group.add_argument(
        "--image-dir",
        type=Path,
        help="Folder with train/ and validation/ class subdirectories.",
    )
    parser.add_argument(
        "--architecture",
        choices=("fer-cnn", "mobilenetv2"),
        default="fer-cnn",
        help="Model family to train. fer-cnn is a compact FER-style CNN; mobilenetv2 uses ImageNet transfer learning.",
    )
    parser.add_argument("--image-size", type=int, default=96)
    parser.add_argument("--batch-size", type=int, default=64)
    parser.add_argument("--head-epochs", type=int, default=12)
    parser.add_argument("--fine-tune-epochs", type=int, default=10)
    parser.add_argument("--learning-rate", type=float, default=1e-3)
    parser.add_argument("--fine-tune-learning-rate", type=float, default=5e-5)
    parser.add_argument("--validation-split", type=float, default=0.15)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--output-dir", type=Path, default=Path("training/exports"))
    parser.add_argument("--run-dir", type=Path, default=Path("training/runs"))
    return parser.parse_args()


def make_rgb(image: tf.Tensor) -> tf.Tensor:
    if image.shape.rank == 2:
        image = image[..., tf.newaxis]
    if image.shape[-1] == 1:
        image = tf.image.grayscale_to_rgb(image)
    return image


def preprocess(image: tf.Tensor, label: tf.Tensor, image_size: int) -> tuple[tf.Tensor, tf.Tensor]:
    image = tf.cast(image, tf.float32)
    image = make_rgb(image)
    image = tf.image.resize(image, [image_size, image_size])
    image = tf.keras.applications.mobilenet_v2.preprocess_input(image)
    return image, label


def augment(image: tf.Tensor, label: tf.Tensor) -> tuple[tf.Tensor, tf.Tensor]:
    image = tf.image.random_flip_left_right(image)
    image = tf.image.random_brightness(image, max_delta=0.15)
    image = tf.image.random_contrast(image, lower=0.75, upper=1.25)
    image = _random_zoom(image)
    return image, label


def _random_zoom(image: tf.Tensor) -> tf.Tensor:
    orig_shape = tf.shape(image)
    scale = tf.random.uniform([], 0.85, 1.0)
    crop_h = tf.cast(tf.cast(orig_shape[0], tf.float32) * scale, tf.int32)
    crop_w = tf.cast(tf.cast(orig_shape[1], tf.float32) * scale, tf.int32)
    image = tf.image.random_crop(image, [crop_h, crop_w, orig_shape[2]])
    image = tf.image.resize(image, [orig_shape[0], orig_shape[1]])
    return image


def _compute_class_weights(labels: np.ndarray) -> dict:
    classes = np.arange(len(APP_LABELS))
    weights = compute_class_weight("balanced", classes=classes, y=labels)
    return dict(enumerate(weights))


def load_from_csv(args: argparse.Namespace) -> tuple[tf.data.Dataset, tf.data.Dataset, dict]:
    df = pd.read_csv(args.csv)
    df["app_label"] = df["emotion"].map(FER2013_ID_TO_LABEL)
    df = df[df["app_label"].isin(APP_LABELS)].copy()
    df["label_id"] = df["app_label"].map({label: idx for idx, label in enumerate(APP_LABELS)})

    pixels = np.stack(
        df["pixels"].map(lambda value: np.fromstring(value, sep=" ", dtype=np.float32)).to_numpy()
    )
    images = pixels.reshape((-1, 48, 48, 1))
    labels = df["label_id"].to_numpy(dtype=np.int64)

    if "Usage" in df.columns:
        train_mask = df["Usage"].eq("Training").to_numpy()
        validation_mask = df["Usage"].ne("Training").to_numpy()
        train_images, validation_images = images[train_mask], images[validation_mask]
        train_labels, validation_labels = labels[train_mask], labels[validation_mask]
    else:
        train_images, validation_images, train_labels, validation_labels = train_test_split(
            images,
            labels,
            test_size=args.validation_split,
            stratify=labels,
            random_state=args.seed,
        )

    class_weights = _compute_class_weights(train_labels)
    train_ds = tf.data.Dataset.from_tensor_slices((train_images, train_labels))
    validation_ds = tf.data.Dataset.from_tensor_slices((validation_images, validation_labels))
    return *prepare_datasets(train_ds, validation_ds, args), class_weights


def load_image_folder_dataset(path: Path) -> tuple[tf.data.Dataset, np.ndarray]:
    image_paths = []
    labels = []
    for folder in sorted(child for child in path.iterdir() if child.is_dir()):
        app_label = FOLDER_LABEL_TO_APP_LABEL.get(folder.name.lower())
        if app_label not in APP_LABELS:
            continue
        label_id = APP_LABELS.index(app_label)
        for image_path in sorted(folder.rglob("*")):
            if image_path.suffix.lower() in SUPPORTED_IMAGE_EXTENSIONS:
                image_paths.append(str(image_path))
                labels.append(label_id)

    if not image_paths:
        raise ValueError(f"No supported images for {APP_LABELS} found in {path}")

    labels_array = np.asarray(labels, dtype=np.int64)
    path_ds = tf.data.Dataset.from_tensor_slices((image_paths, labels_array))

    def read_image(image_path: tf.Tensor, label: tf.Tensor) -> tuple[tf.Tensor, tf.Tensor]:
        image_bytes = tf.io.read_file(image_path)
        image = tf.io.decode_image(image_bytes, channels=3, expand_animations=False)
        image.set_shape([None, None, 3])
        return image, label

    return path_ds.map(read_image, num_parallel_calls=tf.data.AUTOTUNE), labels_array


def load_from_image_dir(args: argparse.Namespace) -> tuple[tf.data.Dataset, tf.data.Dataset, dict]:
    train_path = args.image_dir / "train"
    validation_path = args.image_dir / "validation"
    if not validation_path.exists():
        validation_path = args.image_dir / "test"

    if not train_path.exists():
        raise FileNotFoundError(f"Train folder not found: {train_path}")
    if not validation_path.exists():
        raise FileNotFoundError(
            "Validation folder not found. Expected either "
            f"{args.image_dir / 'validation'} or {args.image_dir / 'test'}"
        )

    train_ds, train_labels = load_image_folder_dataset(train_path)
    validation_ds, _ = load_image_folder_dataset(validation_path)

    class_weights = _compute_class_weights(train_labels)
    return *prepare_datasets(train_ds, validation_ds, args), class_weights


def prepare_datasets(
    train_ds: tf.data.Dataset,
    validation_ds: tf.data.Dataset,
    args: argparse.Namespace,
) -> tuple[tf.data.Dataset, tf.data.Dataset]:
    train_ds = (
        train_ds.shuffle(4096, seed=args.seed)
        .map(lambda image, label: preprocess(image, label, args.image_size), tf.data.AUTOTUNE)
        .map(augment, tf.data.AUTOTUNE)
        .batch(args.batch_size)
        .prefetch(tf.data.AUTOTUNE)
    )
    validation_ds = (
        validation_ds.map(
            lambda image, label: preprocess(image, label, args.image_size),
            tf.data.AUTOTUNE,
        )
        .batch(args.batch_size)
        .prefetch(tf.data.AUTOTUNE)
    )
    return train_ds, validation_ds


def conv_block(
    x: tf.Tensor,
    filters: int,
    dropout_rate: float,
    l2_rate: float = 1e-4,
) -> tf.Tensor:
    for _ in range(2):
        x = tf.keras.layers.Conv2D(
            filters,
            kernel_size=3,
            padding="same",
            use_bias=False,
            kernel_regularizer=tf.keras.regularizers.l2(l2_rate),
        )(x)
        x = tf.keras.layers.BatchNormalization()(x)
        x = tf.keras.layers.Activation("relu")(x)
    x = tf.keras.layers.MaxPooling2D(pool_size=2)(x)
    x = tf.keras.layers.Dropout(dropout_rate)(x)
    return x


def build_fer_cnn(image_size: int, learning_rate: float) -> tuple[tf.keras.Model, None]:
    inputs = tf.keras.Input(shape=(image_size, image_size, 3), name="image")

    x = conv_block(inputs, filters=32, dropout_rate=0.20)
    x = conv_block(x, filters=64, dropout_rate=0.25)
    x = conv_block(x, filters=128, dropout_rate=0.30)
    x = conv_block(x, filters=256, dropout_rate=0.35)

    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dense(
        256,
        use_bias=False,
        kernel_regularizer=tf.keras.regularizers.l2(1e-4),
    )(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Activation("relu")(x)
    x = tf.keras.layers.Dropout(0.45)(x)
    outputs = tf.keras.layers.Dense(len(APP_LABELS), activation="softmax", name="emotion")(x)

    model = tf.keras.Model(inputs=inputs, outputs=outputs)
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    return model, None


def build_mobilenetv2(image_size: int, learning_rate: float) -> tuple[tf.keras.Model, tf.keras.Model]:
    inputs = tf.keras.Input(shape=(image_size, image_size, 3), name="image")
    base_model = tf.keras.applications.MobileNetV2(
        input_tensor=inputs,
        include_top=False,
        weights="imagenet",
        alpha=1.0,
    )
    base_model.trainable = False

    x = base_model.output
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dense(256, activation="relu", kernel_regularizer=tf.keras.regularizers.l2(1e-4))(x)
    x = tf.keras.layers.Dropout(0.4)(x)
    outputs = tf.keras.layers.Dense(len(APP_LABELS), activation="softmax", name="emotion")(x)
    model = tf.keras.Model(inputs=inputs, outputs=outputs)

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    return model, base_model


def build_model(
    architecture: str,
    image_size: int,
    learning_rate: float,
) -> tuple[tf.keras.Model, tf.keras.Model | None]:
    if architecture == "fer-cnn":
        return build_fer_cnn(image_size, learning_rate)
    return build_mobilenetv2(image_size, learning_rate)


def export_tflite(model: tf.keras.Model, output_dir: Path) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

    (output_dir / "emotion_mobilenetv2.tflite").write_bytes(tflite_model)
    (output_dir / "emotion_labels.txt").write_text("\n".join(APP_LABELS) + "\n", encoding="utf-8")


def main() -> None:
    args = parse_args()
    tf.keras.utils.set_random_seed(args.seed)
    args.run_dir.mkdir(parents=True, exist_ok=True)

    if args.csv:
        train_ds, validation_ds, class_weights = load_from_csv(args)
    else:
        train_ds, validation_ds, class_weights = load_from_image_dir(args)

    print(f"Class weights: {class_weights}")

    model, base_model = build_model(args.architecture, args.image_size, args.learning_rate)
    checkpoint_path = args.run_dir / f"best_emotion_{args.architecture}.keras"
    callbacks = [
        tf.keras.callbacks.ModelCheckpoint(
            checkpoint_path,
            monitor="val_accuracy",
            mode="max",
            save_best_only=True,
        ),
        tf.keras.callbacks.EarlyStopping(
            monitor="val_accuracy",
            mode="max",
            patience=5,
            restore_best_weights=True,
        ),
    ]

    model.fit(
        train_ds,
        validation_data=validation_ds,
        epochs=args.head_epochs,
        class_weight=class_weights,
        callbacks=callbacks,
    )

    if base_model is not None:
        base_model.trainable = True
        for layer in base_model.layers[:-40]:
            layer.trainable = False

        model.compile(
            optimizer=tf.keras.optimizers.Adam(args.fine_tune_learning_rate),
            loss="sparse_categorical_crossentropy",
            metrics=["accuracy"],
        )
        model.fit(
            train_ds,
            validation_data=validation_ds,
            epochs=args.fine_tune_epochs,
            class_weight=class_weights,
            callbacks=callbacks,
        )
    elif args.fine_tune_epochs > 0:
        model.compile(
            optimizer=tf.keras.optimizers.Adam(args.fine_tune_learning_rate),
            loss="sparse_categorical_crossentropy",
            metrics=["accuracy"],
        )
        model.fit(
            train_ds,
            validation_data=validation_ds,
            epochs=args.fine_tune_epochs,
            class_weight=class_weights,
            callbacks=callbacks,
        )

    best_model = tf.keras.models.load_model(checkpoint_path)
    export_tflite(best_model, args.output_dir)
    print(f"TFLite model exported to {args.output_dir / 'emotion_mobilenetv2.tflite'}")
    print(f"Labels exported to {args.output_dir / 'emotion_labels.txt'}")


if __name__ == "__main__":
    main()
