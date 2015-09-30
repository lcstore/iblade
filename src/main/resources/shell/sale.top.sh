#!/bin/bash
# srcDir,destFile
function merge_data(){
    src_dir= $1
    dest_file= $2
	filelist=`ls $src_dir`
	for file in $filelist
	do 
	 cat $file > $dest_file
	done
}
v_date=`date +%Y%m%d --date="-0 day"`
if [[ $# -lt 1 ]]; then
	echo "input params[v_site](v_date)"
	exit 1
fi
v_site=$1
if [[ $# -gt 1 ]]; then
	v_date=$2
fi
if [[ $v_site"x" == "jdx" ]]; then
	CATE_CLASS="com.lezo.mall.blade.require.top.JdBestSaleListMain"
    SKU_CLASS="com.lezo.mall.blade.require.top.JdBestSaleSkuMain"
elif [[ $v_site"x" == "amazonx" ]]; then
	CATE_CLASS="com.lezo.mall.blade.require.top.AmazonBestSaleListMain"
    SKU_CLASS="com.lezo.mall.blade.require.top.AmazonBestSaleSkuMain"
else
   echo "input a correct site,unkwon site:"$v_site
   exit 1
fi
JAR_DIR=$PWD"/blade.jar"
CATE_DEST_PATH=$PWD"/data/"$v_date"/"$v_site"/top/cate/"
SKU_DEST_PATH=$PWD"/data/"$v_date"/"$v_site"/top/sku/"
DEST_DATA_FILE=$PWD"/data/"$v_site".sale.top."$v_date".data"

CATE_LOG_PATH=$PWD"/logs/"$v_site".cate."$v_date".out"
SKU_LOG_PATH=$PWD"/logs/"$v_site".sku."$v_date".out"
EXEC_CMD="java -Xms256m -Xmx1024m  -Ddest=$CATE_DEST_PATH -cp $JAR_DIR $CATE_CLASS > $CATE_LOG_PATH"
echo $EXEC_CMD
eval $EXEC_CMD
JOB_STATUS=$?
if [[ $JOB_STATUS -ne 0 ]]; then
	handle_ret JOB_STATUS
	echo -e "exec cate fail:$JOB_STATUS,ready to exit..."
	exit 1;
fi

EXEC_CMD="java -Xms256m -Xmx1024m -Dsrc=$CATE_DEST_PATH  -Ddest=$SKU_DEST_PATH -cp $JAR_DIR $SKU_CLASS > $SKU_LOG_PATH"
echo $EXEC_CMD
eval $EXEC_CMD
JOB_STATUS=$?
if [[ $JOB_STATUS -ne 0 ]]; then
	handle_ret JOB_STATUS
	echo -e "exec sku fail:$JOB_STATUS,ready to exit..."
	exit 1;
fi
EXEC_CMD="merge_data $SKU_DEST_PATH $DEST_DATA_FILE"
echo $EXEC_CMD
eval $EXEC_CMD
JOB_STATUS=$?
if [[ $JOB_STATUS -ne 0 ]]; then
	handle_ret JOB_STATUS
	echo -e "exec sku fail:$JOB_STATUS,ready to exit..."
	exit 1;
fi
echo "done v_site:"$v_site",v_date:"$v_date