import argparse
from pathlib import Path

import numpy as np
import tensorflow as tf
from sklearn.metrics import classification_report, confusion_matrix


APP_LABELS = ["HAPPY", "SAD", "ANGRY", "SURPRISED"]

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
        description="Evaluate an exported FER2013 TFLite emotion model on an image test set."
    )
    parser.add_argument(
        "--model",
        type=Path,
        default=Path("app/src/main/assets/emotion_mobilenetv2.tflite"),
        help="Path to the exported TFLite model.",
    )
    parser.add_argument(
        "--test-dir",
        type=Path,
        default=Path("training/data/fer2013/test"),
        help="Folder with class subdirectories for the test split.",
    )
    parser.add_argument("--image-size", type=int, default=96)
    parser.add_argument("--batch-size", type=int, default=64)
    return parser.parse_args()


def preprocess(image: tf.Tensor, image_size: int) -> tf.Tensor:
    image = tf.cast(image, tf.float32)
    image = tf.image.resize(image, [image_size, image_size])
    return tf.keras.applications.mobilenet_v2.preprocess_input(image)


def load_test_dataset(args: argparse.Namespace) -> tuple[tf.data.Dataset, list[str]]:
    image_paths = []
    labels = []
    class_names = []
    for folder in sorted(path for path in args.test_dir.iterdir() if path.is_dir()):
        app_label = FOLDER_LABEL_TO_APP_LABEL.get(folder.name.lower())
        if app_label not in APP_LABELS:
            continue
        class_names.append(folder.name)
        label_id = APP_LABELS.index(app_label)
        for image_path in sorted(folder.rglob("*")):
            if image_path.suffix.lower() in {".bmp", ".gif", ".jpeg", ".jpg", ".png"}:
                image_paths.append(str(image_path))
                labels.append(label_id)

    if not image_paths:
        raise ValueError(f"No supported FER2013 images found in {args.test_dir}")

    path_ds = tf.data.Dataset.from_tensor_slices((image_paths, np.asarray(labels, dtype=np.int64)))

    def read_image(image_path: tf.Tensor, label: tf.Tensor) -> tuple[tf.Tensor, tf.Tensor]:
        image_bytes = tf.io.read_file(image_path)
        image = tf.io.decode_image(image_bytes, channels=3, expand_animations=False)
        image.set_shape([None, None, 3])
        return preprocess(image, args.image_size), label

    test_ds = (
        path_ds.map(read_image, num_parallel_calls=tf.data.AUTOTUNE)
        .batch(args.batch_size)
        .prefetch(tf.data.AUTOTUNE)
    )
    return test_ds, class_names


def dequantize_output(output: np.ndarray, output_details: dict) -> np.ndarray:
    scale, zero_point = output_details.get("quantization", (0.0, 0))
    if scale:
        return scale * (output.astype(np.float32) - zero_point)
    return output


def set_input(interpreter: tf.lite.Interpreter, input_details: dict, batch: np.ndarray) -> None:
    expected_shape = input_details["shape"]
    if expected_shape[0] != batch.shape[0]:
        interpreter.resize_tensor_input(input_details["index"], batch.shape, strict=False)
        interpreter.allocate_tensors()
        input_details.update(interpreter.get_input_details()[0])

    if input_details["dtype"] == np.uint8:
        scale, zero_point = input_details["quantization"]
        batch = (batch / scale + zero_point).round().clip(0, 255).astype(np.uint8)
    elif input_details["dtype"] == np.int8:
        scale, zero_point = input_details["quantization"]
        batch = (batch / scale + zero_point).round().clip(-128, 127).astype(np.int8)
    else:
        batch = batch.astype(input_details["dtype"])

    interpreter.set_tensor(input_details["index"], batch)


def evaluate_tflite(args: argparse.Namespace) -> tuple[np.ndarray, np.ndarray]:
    test_ds, _class_names = load_test_dataset(args)
    interpreter = tf.lite.Interpreter(model_path=str(args.model))
    interpreter.allocate_tensors()
    input_details = interpreter.get_input_details()[0]

    y_true = []
    y_pred = []
    for images, labels in test_ds:
        batch = images.numpy()
        set_input(interpreter, input_details, batch)
        interpreter.invoke()
        output_details = interpreter.get_output_details()[0]
        output = dequantize_output(
            interpreter.get_tensor(output_details["index"]),
            output_details,
        )
        output = output[:, : len(APP_LABELS)]
        y_true.extend(labels.numpy().tolist())
        y_pred.extend(np.argmax(output, axis=1).tolist())

    return np.asarray(y_true), np.asarray(y_pred)


def main() -> None:
    args = parse_args()
    if not args.model.exists():
        raise FileNotFoundError(f"Model not found: {args.model}")
    if not args.test_dir.exists():
        raise FileNotFoundError(f"Test folder not found: {args.test_dir}")

    y_true, y_pred = evaluate_tflite(args)
    accuracy = float(np.mean(y_true == y_pred))
    print(f"Model: {args.model}")
    print(f"Test directory: {args.test_dir}")
    print(f"Samples: {len(y_true)}")
    print(f"Accuracy: {accuracy:.4f}")
    print()
    print("Classification report:")
    print(classification_report(y_true, y_pred, target_names=APP_LABELS, digits=4))
    print("Confusion matrix rows=true, columns=predicted:")
    print("Labels:", ", ".join(APP_LABELS))
    print(confusion_matrix(y_true, y_pred, labels=list(range(len(APP_LABELS)))))


if __name__ == "__main__":
    main()
