import argparse
from pathlib import Path

import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.model_selection import train_test_split


APP_LABELS = ["HAPPY", "SAD", "ANGRY", "SURPRISED", "NEUTRAL", "FEAR", "DISGUST"]

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


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Fine-tune MobileNetV2 on FER2013 and export a TFLite model."
    )
    input_group = parser.add_mutually_exclusive_group(required=True)
    input_group.add_argument("--csv", type=Path, help="Path to Kaggle fer2013.csv.")
    input_group.add_argument(
        "--image-dir",
        type=Path,
        help="Folder with train/ and validation/ class subdirectories.",
    )
    parser.add_argument("--image-size", type=int, default=96)
    parser.add_argument("--batch-size", type=int, default=64)
    parser.add_argument("--head-epochs", type=int, default=12)
    parser.add_argument("--fine-tune-epochs", type=int, default=10)
    parser.add_argument("--learning-rate", type=float, default=1e-3)
    parser.add_argument("--fine-tune-learning-rate", type=float, default=1e-5)
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
    image = tf.image.random_brightness(image, max_delta=0.08)
    image = tf.image.random_contrast(image, lower=0.85, upper=1.15)
    return image, label


def load_from_csv(args: argparse.Namespace) -> tuple[tf.data.Dataset, tf.data.Dataset]:
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

    train_ds = tf.data.Dataset.from_tensor_slices((train_images, train_labels))
    validation_ds = tf.data.Dataset.from_tensor_slices((validation_images, validation_labels))
    return prepare_datasets(train_ds, validation_ds, args)


def remap_folder_labels(dataset: tf.data.Dataset, class_names: list[str]) -> tf.data.Dataset:
    folder_to_index = {name.lower(): idx for idx, name in enumerate(class_names)}
    old_to_new = {}
    for folder_name, old_index in folder_to_index.items():
        app_label = FOLDER_LABEL_TO_APP_LABEL.get(folder_name)
        if app_label is not None:
            old_to_new[old_index] = APP_LABELS.index(app_label)

    keys = tf.constant(list(old_to_new.keys()), dtype=tf.int64)
    values = tf.constant(list(old_to_new.values()), dtype=tf.int64)
    table = tf.lookup.StaticHashTable(
        tf.lookup.KeyValueTensorInitializer(keys, values),
        default_value=-1,
    )

    def mapper(image: tf.Tensor, label: tf.Tensor) -> tuple[tf.Tensor, tf.Tensor]:
        return image, table.lookup(tf.cast(label, tf.int64))

    return dataset.map(mapper, num_parallel_calls=tf.data.AUTOTUNE).filter(
        lambda _image, label: label >= 0
    )


def load_from_image_dir(args: argparse.Namespace) -> tuple[tf.data.Dataset, tf.data.Dataset]:
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

    train_raw = tf.keras.utils.image_dataset_from_directory(
        train_path,
        color_mode="rgb",
        image_size=(args.image_size, args.image_size),
        batch_size=None,
        shuffle=True,
        seed=args.seed,
    )
    validation_raw = tf.keras.utils.image_dataset_from_directory(
        validation_path,
        color_mode="rgb",
        image_size=(args.image_size, args.image_size),
        batch_size=None,
        shuffle=False,
    )

    train_ds = remap_folder_labels(train_raw, train_raw.class_names)
    validation_ds = remap_folder_labels(validation_raw, validation_raw.class_names)
    return prepare_datasets(train_ds, validation_ds, args)


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


def build_model(image_size: int, learning_rate: float) -> tuple[tf.keras.Model, tf.keras.Model]:
    inputs = tf.keras.Input(shape=(image_size, image_size, 3), name="image")
    base_model = tf.keras.applications.MobileNetV2(
        input_tensor=inputs,
        include_top=False,
        weights="imagenet",
        alpha=0.75,
    )
    base_model.trainable = False

    x = base_model.output
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dropout(0.25)(x)
    outputs = tf.keras.layers.Dense(len(APP_LABELS), activation="softmax", name="emotion")(x)
    model = tf.keras.Model(inputs=inputs, outputs=outputs)

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    return model, base_model


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
        train_ds, validation_ds = load_from_csv(args)
    else:
        train_ds, validation_ds = load_from_image_dir(args)

    model, base_model = build_model(args.image_size, args.learning_rate)
    checkpoint_path = args.run_dir / "best_emotion_mobilenetv2.keras"
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
        callbacks=callbacks,
    )

    base_model.trainable = True
    for layer in base_model.layers[:-30]:
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
        callbacks=callbacks,
    )

    best_model = tf.keras.models.load_model(checkpoint_path)
    export_tflite(best_model, args.output_dir)
    print(f"TFLite model exported to {args.output_dir / 'emotion_mobilenetv2.tflite'}")
    print(f"Labels exported to {args.output_dir / 'emotion_labels.txt'}")


if __name__ == "__main__":
    main()
