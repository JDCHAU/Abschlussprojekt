#!/bin/bash

# Job settings 
#SBATCH --job-name=my-job
#SBATCH --time=00:30:00

# 
PORT=20001                 
INNER=9715                
IMG="/sif_pool/spacy.sif"   
INTOIMAGE="cd /usr/src/app"
UVI="uvicorn textimager_duui_spacy:app"   

apptainer exec "$IMG" \
  sh -c "$INTOIMAGE && $UVI --host 0.0.0.0 --port $INNER" &

PID=$!


socat TCP-LISTEN:$PORT,reuseaddr,fork TCP:127.0.0.1:$INNER &

PID_SOCAT=$!


trap 'kill $PID $PID_SOCAT 2>/dev/null' EXIT

wait $PID
