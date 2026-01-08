#!/usr/bin/env python3

import argparse
import csv
import datetime as dt
import os
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import time
from pathlib import Path
from typing import Dict, List, Optional, Tuple


ARTHAS_CLASSLOADER_CLASSNAME = "com.taobao.arthas.agent.ArthasClassloader"
JMAP_HISTO_LINE_RE = re.compile(r"^\s*\d+:\s+(?P<instances>\d+)\s+(?P<bytes>\d+)\s+(?P<class>\S+)\s*$")
ARTHAS_LOG_RANDOM_TELNET_PORT_RE = re.compile(r"generate random telnet port: (\d+)")
ARTHAS_LOG_BIND_TELNET_PORT_RE = re.compile(r"try to bind telnet server, host: .* port: (\d+)")


def find_repo_root(start: Path) -> Path:
    for path in [start] + list(start.parents):
        if (path / "pom.xml").is_file() and (path / "bin" / "as.sh").exists():
            return path
    raise RuntimeError(f"无法定位仓库根目录(未找到 pom.xml/bin/as.sh)，start={start}")


def which_or_throw(name: str) -> str:
    value = shutil.which(name)
    if value:
        return value
    raise RuntimeError(f"未找到命令: {name} (请先安装并确保在 PATH 中)")


def resolve_java_bin(name: str) -> str:
    direct = shutil.which(name)
    if direct:
        return direct
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidate = Path(java_home) / "bin" / name
        if candidate.exists():
            return str(candidate)
    raise RuntimeError(f"未找到命令: {name} (PATH 与 JAVA_HOME/bin 中都不存在)")


def wait_for_tcp_port(host: str, port: int, timeout_seconds: int) -> None:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        try:
            with socket.create_connection((host, port), timeout=1):
                return
        except OSError:
            time.sleep(0.5)
    raise TimeoutError(f"等待端口监听超时: {host}:{port}")


def run_checked(cmd: List[str], *, env: Optional[Dict[str, str]] = None, timeout_seconds: Optional[int] = None,
                stdout_path: Optional[Path] = None) -> None:
    stdout = None
    if stdout_path is not None:
        stdout_path.parent.mkdir(parents=True, exist_ok=True)
        stdout = open(stdout_path, "wb")
    try:
        subprocess.run(
            cmd,
            env=env,
            stdout=stdout if stdout is not None else None,
            stderr=subprocess.STDOUT if stdout is not None else None,
            timeout=timeout_seconds,
            check=True,
        )
    finally:
        if stdout is not None:
            stdout.close()


def parse_arthas_classloader_instances(jmap_histo_output: str) -> Tuple[int, Optional[str]]:
    matched_line: Optional[str] = None
    instances = 0
    for line in jmap_histo_output.splitlines():
        if ARTHAS_CLASSLOADER_CLASSNAME not in line:
            continue
        m = JMAP_HISTO_LINE_RE.match(line)
        if not m:
            matched_line = line.strip()
            continue
        if m.group("class") != ARTHAS_CLASSLOADER_CLASSNAME:
            continue
        matched_line = line.strip()
        instances = int(m.group("instances"))
        break
    return instances, matched_line


def run_jmap_histo_live(jmap_bin: str, pid: int, timeout_seconds: int, stderr_log: Path) -> None:
    stderr_log.parent.mkdir(parents=True, exist_ok=True)
    with open(stderr_log, "wb") as err:
        subprocess.run(
            [jmap_bin, "-histo:live", str(pid)],
            stdout=subprocess.DEVNULL,
            stderr=err,
            timeout=timeout_seconds,
            check=True,
        )


def wait_for_telnet_port_from_arthas_log(arthas_log: Path, start_offset: int, timeout_seconds: int) -> int:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        if not arthas_log.exists():
            time.sleep(0.2)
            continue

        with open(arthas_log, "rb") as f:
            f.seek(start_offset)
            chunk = f.read()
        if not chunk:
            time.sleep(0.2)
            continue

        text = chunk.decode("utf-8", errors="replace")
        matches = ARTHAS_LOG_RANDOM_TELNET_PORT_RE.findall(text)
        if matches:
            return int(matches[-1])

        matches = ARTHAS_LOG_BIND_TELNET_PORT_RE.findall(text)
        if matches:
            return int(matches[-1])

        time.sleep(0.2)

    raise TimeoutError(f"未能从 arthas.log 解析 telnet 端口: {arthas_log}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Arthas telnet 循环 stop 后类加载器泄露检查（python + expect + jmap）")
    parser.add_argument("--iterations", type=int, default=10, help="循环次数(默认: 10)")
    parser.add_argument("--warmup", type=int, default=2, help="预热轮数，不参与判定(默认: 2)")
    parser.add_argument("--threshold", type=int, default=1, help="允许的抖动阈值(默认: 1)")
    parser.add_argument("--attach-timeout", type=int, default=90, help="as.sh attach 超时秒数(默认: 90)")
    parser.add_argument("--port-wait-timeout", type=int, default=30, help="等待 telnet 端口监听超时秒数(默认: 30)")
    parser.add_argument("--expect-timeout", type=int, default=30, help="expect 每条命令等待超时秒数(默认: 30)")
    parser.add_argument("--jmap-timeout", type=int, default=120, help="单次 jmap 超时秒数(默认: 120)")
    parser.add_argument("--post-stop-sleep", type=float, default=1.0, help="stop 后等待秒数再做 jmap(默认: 1.0)")
    parser.add_argument("--work-dir", type=str, default="", help="工作目录(默认: 临时目录；若指定则在其下创建 run-时间戳 子目录)")
    parser.add_argument("--arthas-bin-dir", type=str, default="", help="arthas-bin 目录(默认: packaging/target/arthas-bin)")
    args = parser.parse_args()

    if args.iterations <= 0:
        raise SystemExit("--iterations 必须 > 0")
    if args.warmup < 0:
        raise SystemExit("--warmup 必须 >= 0")
    if args.warmup >= args.iterations:
        raise SystemExit("--warmup 必须 < --iterations")
    if args.threshold < 0:
        raise SystemExit("--threshold 必须 >= 0")

    repo_root = find_repo_root(Path(__file__).resolve())

    if args.work_dir:
        base = (repo_root / args.work_dir).resolve() if not os.path.isabs(args.work_dir) else Path(args.work_dir)
        run_dir = base / f"run-{dt.datetime.now().strftime('%Y%m%d-%H%M%S')}"
        run_dir.mkdir(parents=True, exist_ok=True)
    else:
        run_dir = Path(tempfile.mkdtemp(prefix="arthas-telnet-stop-leak-"))

    logs_dir = run_dir / "logs"
    logs_dir.mkdir(parents=True, exist_ok=True)
    home_dir = (run_dir / "home").resolve()
    home_dir.mkdir(parents=True, exist_ok=True)

    print(f"[INFO] repo_root={repo_root}")
    print(f"[INFO] run_dir={run_dir}")

    expect_bin = which_or_throw("expect")
    _ = which_or_throw("telnet")
    bash_bin = which_or_throw("bash")
    java_bin = resolve_java_bin("java")
    jmap_bin = resolve_java_bin("jmap")

    arthas_bin_dir = Path(args.arthas_bin_dir) if args.arthas_bin_dir else repo_root / "packaging" / "target" / "arthas-bin"
    if not arthas_bin_dir.is_absolute():
        arthas_bin_dir = (repo_root / arthas_bin_dir).resolve()
    as_sh = arthas_bin_dir / "as.sh"
    math_game_jar = arthas_bin_dir / "math-game.jar"
    if not as_sh.exists():
        raise RuntimeError(f"未找到 {as_sh}，请先构建 packaging：mvn -pl packaging -am package")
    if not math_game_jar.exists():
        raise RuntimeError(f"未找到 {math_game_jar}，请先构建 packaging：mvn -pl packaging -am package")

    expect_script = repo_root / "integration-test" / "telnet-stop-leak" / "arthas_telnet.exp"
    commands_file = repo_root / "integration-test" / "telnet-stop-leak" / "commands.txt"
    if not expect_script.exists():
        raise RuntimeError(f"未找到 expect 脚本: {expect_script}")
    if not commands_file.exists():
        raise RuntimeError(f"未找到命令列表: {commands_file}")

    env_for_attach = os.environ.copy()
    env_for_attach["HOME"] = str(home_dir)

    target_log = logs_dir / "math-game.log"
    print(f"[INFO] 启动目标应用: {math_game_jar}")
    target_env = os.environ.copy()
    target_env["HOME"] = str(home_dir)
    with open(target_log, "wb") as out:
        target_proc = subprocess.Popen(
            [java_bin, "-Xmx50m", f"-Duser.home={home_dir}", "-jar", str(math_game_jar)],
            stdout=out,
            stderr=subprocess.STDOUT,
            env=target_env,
        )

    try:
        pid = int(target_proc.pid)
        print(f"[INFO] target_pid={pid}")

        results_csv = run_dir / "results.csv"
        with open(results_csv, "w", newline="", encoding="utf-8") as csv_file:
            writer = csv.DictWriter(
                csv_file,
                fieldnames=[
                    "iteration",
                    "telnet_port",
                    "http_port",
                    "arthas_classloader_instances",
                    "matched_histo_line",
                ],
            )
            writer.writeheader()

            min_so_far: Optional[int] = None

            for i in range(1, args.iterations + 1):
                if target_proc.poll() is not None:
                    raise RuntimeError(f"目标 JVM 已退出(exit={target_proc.returncode})，详见: {target_log}")

                requested_telnet_port = 0
                requested_http_port = -1

                arthas_log = home_dir / "logs" / "arthas" / "arthas.log"
                start_offset = arthas_log.stat().st_size if arthas_log.exists() else 0

                attach_log = logs_dir / "attach" / f"attach-{i:03d}.log"
                attach_cmd = [
                    bash_bin,
                    str(as_sh),
                    "--attach-only",
                    "--arthas-home",
                    str(arthas_bin_dir),
                    "--target-ip",
                    "127.0.0.1",
                    "--telnet-port",
                    str(requested_telnet_port),
                    "--http-port",
                    str(requested_http_port),
                    str(pid),
                ]

                print(f"[INFO] [{i}/{args.iterations}] attach: telnet_port=0(http=-1) pid={pid}")
                run_checked(attach_cmd, env=env_for_attach, timeout_seconds=args.attach_timeout, stdout_path=attach_log)

                telnet_port = wait_for_telnet_port_from_arthas_log(arthas_log, start_offset, args.port_wait_timeout)
                try:
                    wait_for_tcp_port("127.0.0.1", telnet_port, args.port_wait_timeout)
                except PermissionError:
                    # 某些沙箱环境可能禁用 socket connect，依赖 expect 自身超时兜底即可。
                    pass

                transcript = logs_dir / "telnet" / f"telnet-{i:03d}.log"
                expect_run_log = logs_dir / "telnet" / f"expect-run-{i:03d}.log"
                expect_cmd = [
                    expect_bin,
                    str(expect_script),
                    "127.0.0.1",
                    str(telnet_port),
                    str(commands_file),
                    str(args.expect_timeout),
                    str(transcript),
                ]
                run_checked(expect_cmd, timeout_seconds=(args.expect_timeout * 20), stdout_path=expect_run_log)

                time.sleep(args.post_stop_sleep)

                jmap_err = logs_dir / "jmap" / f"jmap-live-{i:03d}.err"
                run_jmap_histo_live(jmap_bin, pid, args.jmap_timeout, jmap_err)

                jmap_histo_out = subprocess.check_output(
                    [jmap_bin, "-histo", str(pid)],
                    stderr=subprocess.STDOUT,
                    timeout=args.jmap_timeout,
                    text=True,
                    encoding="utf-8",
                    errors="replace",
                )
                instances, matched_line = parse_arthas_classloader_instances(jmap_histo_out)

                writer.writerow(
                    {
                        "iteration": i,
                        "telnet_port": telnet_port,
                        "http_port": requested_http_port,
                        "arthas_classloader_instances": instances,
                        "matched_histo_line": matched_line or "",
                    }
                )
                csv_file.flush()

                print(f"[INFO] [{i}/{args.iterations}] {ARTHAS_CLASSLOADER_CLASSNAME} instances={instances}")

                if i <= args.warmup:
                    continue

                min_so_far = instances if min_so_far is None else min(min_so_far, instances)
                if instances > min_so_far + args.threshold:
                    raise RuntimeError(
                        "检测到 ArthasClassloader 实例数异常增长: "
                        f"iteration={i} instances={instances} min_so_far={min_so_far} threshold={args.threshold}. "
                        f"详见: {results_csv} / {logs_dir}"
                    )

        print(f"[INFO] PASS: {results_csv}")
        return 0
    finally:
        target_proc.terminate()
        try:
            target_proc.wait(timeout=10)
        except subprocess.TimeoutExpired:
            target_proc.kill()
            target_proc.wait(timeout=10)


if __name__ == "__main__":
    sys.exit(main())
