argv=$*
files=''

find_files(){
    if [ -z "${MDOC_HOME}" ];then
       echo "{"
       echo "\"type\": \"error\","
       echo "\"title\": \"请设置环境变量MDOC_HOME\""
       echo "}"
       echo "]}"
       exit 1
    fi
    if [ ! -d "${MDOC_HOME}/docs" ];then
       echo "{"
       echo "\"type\": \"error\","
       echo "\"title\": \"\\\"${MDOC_HOME}/docs\\\" 目录不存在\""
       echo "}"
       echo "]}"
       exit 1
    fi
    cd "${MDOC_HOME}/docs"

    local final_expr="ls -ltr *.md | awk '{print \$9}'"
    if [ $# -gt 0 ];then
        for i in $*
        do
            final_expr="${final_expr}| xargs grep -i ${i} | awk -F':' '{print \$1}' | uniq "
        done

        # 排序表达式: 统计第一行匹配关键字个数，将匹配个数大的放在前面
        # 第一步: 输入"文件名",输出"文件名 匹配个数"
        # 第二步: 按照 "匹配个数" 倒序排序
        # 第三步: 去掉 "匹配个数" 字段，只保留"文件名"
        # 由于ls -ltr 是按照编辑时间倒序排序的，所以最终排序等级：标题匹配个数倒序->最后编辑倒序
        local egrep_expr=$(echo "$*" | sed "s/[[:blank:]]/|/g")
        local sort_expr="awk '{system(\"egrep -io \\\"${egrep_expr}\\\" <<< \`head -1 \"\$1 \"\`|wc -l | xargs echo \"\$1)}' | sort -rk 2 | awk '{print \$1}'"

        final_expr="${final_expr} | ${sort_expr}"
    fi
    # echo "${final_expr}"
    files=`eval "${final_expr}"`

##### 由于执行速度不够，已经由前面排序表达式完成了这个功能，而且只使用了第一个关键字匹配，上面表达式按照最大匹配度排序
# #把标题带有关键字的文件的移动到前面
# filesWithKeywordArray=()
#
# count=0
# filesArray=( $files )
##  echo $1
##  echo ${filesArray[@]}
#
#    for i in ${filesArray[@]}
#    do
#   filename=$(head -1 $i)
#   filename="$(tr [A-Z] [a-z] <<< "$filename")"  #转换大小写
#   keyword="$(tr [A-Z] [a-z] <<< "$1")"      #转换大小写
#   if [[ ${filename} == *"$keyword"* ]]; then
#     filesWithKeywordArray+="$i "
#     unset filesArray[$count]
#        fi
#        let count=count+1
#    done
#
# filesArray=( ${filesWithKeywordArray[@]} ${filesArray[@]} )
# files=`echo ${filesArray[@]}`
}

output(){
    if [ "${files}" = "" ];then
       echo "{"
       echo "\"title\": \"没有找到符合条件的文档\","
       echo "}"
       echo "]}"
       exit
    fi
    local separator=""
    for i in ${files}
    do
      printf '%s' ${separator}
      separator=","
      echo "{"
      echo "\"uid\": \"$i\","
      echo "\"type\": \"file\","
      echo "\"title\": \"`head -1 ${i}`\","
      echo "\"arg\": \"${MDOC_HOME}/docs/$i\""
      echo "}"
    done
    echo "]}"
}

mo(){
    echo "{ \"items\":["
    find_files $*
    output
}

mo $*