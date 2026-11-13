#!/usr/bin/env python3
import argparse
import os
import re
import subprocess
import sys


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--start", type=int, required=True)
    parser.add_argument("--end", type=int, required=True)
    parser.add_argument("--stage", required=True)
    parser.add_argument("gradle_args", nargs=argparse.REMAINDER)
    args = parser.parse_args()

    gradle_args = list(args.gradle_args)
    if gradle_args and gradle_args[0] == "--":
        gradle_args = gradle_args[1:]
    if not gradle_args:
        print("::error::Thiếu tác vụ Gradle cần chạy.")
        return 2
    if not (0 <= args.start < args.end <= 100):
        print("::error::Khoảng phần trăm Gradle không hợp lệ.")
        return 2

    progress_script = os.path.join(
        os.environ.get("RUNNER_TEMP", "."), "coclio-apk-progress.sh"
    )
    command = [
        "gradle",
        "--no-daemon",
        "--build-cache",
        "--parallel",
        "--console=plain",
        "--init-script",
        ".github/scripts/coclio-progress.init.gradle",
        *gradle_args,
    ]

    print(
        f"[COC-LIO] Bắt đầu {args.stage}; đang theo dõi số tác vụ Gradle hoàn tất.",
        flush=True,
    )
    try:
        process = subprocess.Popen(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
            errors="replace",
        )
    except OSError as exc:
        print(f"::error::Không thể khởi chạy Gradle: {exc}")
        return 127

    total_tasks = None
    completed_tasks = 0
    last_percent = args.start

    assert process.stdout is not None
    for raw_line in process.stdout:
        print(raw_line, end="", flush=True)

        total_match = re.search(r"COCLIO_TASK_TOTAL:(\d+)", raw_line)
        if total_match:
            total_tasks = int(total_match.group(1))
            print(
                f"[COC-LIO] Tổng số tác vụ trong giai đoạn: {total_tasks}.",
                flush=True,
            )
            continue

        if "COCLIO_TASK_DONE:" in raw_line:
            completed_tasks += 1
            if total_tasks and total_tasks > 0:
                span = args.end - args.start - 1
                percent = args.start + (completed_tasks * span // total_tasks)
                percent = min(args.end - 1, max(last_percent, percent))
                if percent > last_percent:
                    stage = (
                        f"{args.stage}: {completed_tasks}/{total_tasks} "
                        "tác vụ đã xử lý"
                    )
                    subprocess.run(
                        ["bash", progress_script, str(percent), stage],
                        check=False,
                    )
                    last_percent = percent

    return_code = process.wait()
    if return_code != 0:
        print(
            f"[COC-LIO] {args.stage} dừng tại "
            f"{completed_tasks}/{total_tasks or '?'} tác vụ.",
            flush=True,
        )
        return return_code

    if total_tasks is None:
        print(
            "[COC-LIO] Gradle không cung cấp số tác vụ; "
            "sẽ dùng mốc hoàn tất của giai đoạn.",
            flush=True,
        )
    else:
        print(
            f"[COC-LIO] Hoàn tất {completed_tasks}/{total_tasks} "
            "tác vụ Gradle.",
            flush=True,
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())
