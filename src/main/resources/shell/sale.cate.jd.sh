#!/bin/bash

v_date=`date +%Y%m%d --date="-0 day"`
if [[ $# -gt 0 ]]; then
	v_date=$1
fi
JAR_DIR=$PWD"/blade.jar"
CLASS_NAME="com.lezo.mall.blade.require.top.JdBestSaleListMain"
DEST_PATH=$PWD"/data/"$v_date"/jd/top/cate/"
LOG_PATH=$PWD"/logs/jd.cate."$v_date".out"
EXEC_CMD="nohup java -Xms256m -Xmx1024m  -Ddest=$DEST_PATH -Dsrc=$SRC_PATH -cp $JAR_DIR $CLASS_NAME > $LOG_PATH & ";
echo $EXEC_CMD
eval $EXEC_CMD