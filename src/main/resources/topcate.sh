#!/bin/bash
if [[ $# -lt 2 ]]; then
	echo "input args.etc[CLASS_NAME,DEST_PATH]LOG_PATH,SRC_PATH"
	exit 1
fi
CLASS_NAME=$1
DEST_PATH=$2
LOG_PATH='nohup.out'
SRC_PATH=''
if [[ $# -gt 2 ]]; then
	LOG_PATH=$3
fi
if [[ $# -gt 3 ]]; then
	SRC_PATH=$4
fi
EXEC_CMD="nohup java -Xms256m -Xmx1024m  -Ddest=$DEST_PATH -Dsrc=$SRC_PATH -cp blade.jar $CLASS_NAME > $LOG_PATH & ";
echo $EXEC_CMD
eval $EXEC_CMD