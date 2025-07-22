#!/bin/bash

# Job settings (可选，SLURM使用时才需要)
#SBATCH --job-name=my-job
#SBATCH --time=00:30:00

# 自定义变量（你可以从命令行传参也可以写死）
PORT=20001                  # 宿主机端口（socat监听）
INNER=9715                 # 容器内端口（FastAPI监听）
IMG="/sif_pool/spacy.sif"   # Apptainer 镜像路径
INTOIMAGE="cd /usr/src/app"
UVI="uvicorn textimager_duui_spacy:app"     # FastAPI 启动命令

# 启动 Apptainer 容器中的 FastAPI 服务
apptainer exec "$IMG" \
  sh -c "$INTOIMAGE && $UVI --host 0.0.0.0 --port $INNER" &

PID=$!

# 使用 socat 将宿主端口转发到容器内部端口
socat TCP-LISTEN:$PORT,reuseaddr,fork TCP:127.0.0.1:$INNER &

PID_SOCAT=$!

# 设置退出时自动清理
trap 'kill $PID $PID_SOCAT 2>/dev/null' EXIT

# 等待 FastAPI 服务退出
wait $PID
