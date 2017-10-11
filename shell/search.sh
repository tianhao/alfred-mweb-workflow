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
    local final_expr="ls -lt *.md | awk '{print \$9}'"
    for i in $*
    do
        final_expr="${final_expr}| xargs grep -i ${i} | awk -F':' '{print \$1}' | uniq "
    done
    files=`eval "${final_expr}"`
    
    

    #把标题带有关键字的文件的移动到前面
    filesWithKeywordArray=()
    
    count=0
    filesArray=( $files )
#    echo $1
#    echo ${filesArray[@]}
     
    for i in ${filesArray[@]} 
    do
        filename=$(head -1 $i)
        filename="$(tr [A-Z] [a-z] <<< "$filename")"    #转换大小写
        keyword="$(tr [A-Z] [a-z] <<< "$1")"            #转换大小写
        if [[ ${filename} == *"$keyword"* ]]; then
            filesWithKeywordArray+="$i "
            unset filesArray[$count]
        fi
        let count=count+1
    done
    
    filesArray=( ${filesWithKeywordArray[@]} ${filesArray[@]} )
    files=`echo ${filesArray[@]}`
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
