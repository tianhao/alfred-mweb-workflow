# 环境变量和目录检查
if [ -z "${MDOC_HOME}" ];then
   echo "{ \"items\":["
   echo "{"
   echo "\"type\": \"error\","
   echo "\"title\": \"请设置环境变量MDOC_HOME\""
   echo "}"
   echo "]}"
   exit 1
fi
if [ ! -d "${MDOC_HOME}/docs" ];then
   echo "{ \"items\":["
   echo "{"
   echo "\"type\": \"error\","
   echo "\"title\": \"\\\"${MDOC_HOME}/docs\\\" 目录不存在\""
   echo "}"
   echo "]}"
   exit 1
fi
cd "${MDOC_HOME}/docs"

declare -a tag_arr # 标签数组
declare -a header_arr # 标题数组
declare -a category_arr # 分类数组
declare -a keyword_arr # 关键字数组
next_input=0 # 下一步输入参数归类，0:keyword, 1: tag, 2: header, 3: category
last_input=0 # 最后一次输入的参数归类；与next_input 相同
end_option=1 #　是否终止了除 keyword 以外类型的参数输入: 单最后一个字符为空格表示输出完成了
end_char="" # 最后一个参数最后输入的字符

get_params(){
    while [ $# -gt 0 ]; do
        case "$1" in
            -t)
#                echo "$1"
                next_input=1
                last_input=${next_input}
                shift;;
            -h)
#                echo "$1"
                next_input=2
                last_input=${next_input}
                shift;;
#            -c|--category)
#                echo "$1"
#                next_input=3
#                last_input=${next_input}
#                shift;;
            *)
#                echo "$1"
                str=${1//，/,} # 中文逗号改成英文逗号
                n=${#str}
                end_char=${str:$((n-1))} # 记录最后一个字符
                # 按照 next_input 指示，将参数放到对应的数组中。
                case "${next_input}" in
                    1) IFS=","; for i in ${str};do tag_arr+=("$i"); done;unset IFS ;;
                    2) IFS=","; for i in ${str};do header_arr+=("$i"); done;unset IFS ;;
                    3) IFS=","; for i in ${str};do category_arr+=("$i"); done;unset IFS ;;
                    *) keyword_arr+=("$str") ;;
                esac
                last_input=${next_input} # 保存最后一次输入的参数类型
                next_input=0;
                shift;;
        esac
    done
}

get_params $*
#echo tag_arr=${tag_arr[@]}
#echo header_arr=${header_arr[@]}
#echo category_arr=${category_arr[@]}
#echo keyword_arr=${keyword_arr[@]}
#echo next_input=${next_input}
#echo last_input=${last_input}

# 输出tag列表的函数
output_tags(){
    if [ "${filtered_tags}" = "" ];then
       echo "{\"items\":["
       echo "{"
       echo "\"title\": \"没有相关tag\","
       echo "}"
       echo "]}"
       exit
    fi
    local separator=""
    echo "{\"items\":["
    for i in ${filtered_tags}
    do
      printf '%s' ${separator}
      separator=","
      echo "{"
      echo "\"title\": \"tag: ${i}\","
      echo "\"autocomplete\": \"${1}${i},\","
      echo "\"valid\":\"no\""
      echo "}"
    done
    echo "]}"
    exit
}

# 输出文档列表的函数
output_files(){
    if [ "${files}" = "" ];then
        echo "{\"items\":["
        echo "{"
        echo "\"title\": \"没有找到符合条件的文档\","
        echo "}"
        echo "]}"
        exit
    fi
    local separator=""
    echo "{\"items\":["
    for i in ${files}
    do
        printf '%s' ${separator}
        separator=","
        h="$(head -1 "${i}"| sed 's/\\/\\\\/g' | sed 's/"/\\"/g')"
        echo "{"
        echo "\"type\": \"file\","
        echo "\"title\": \"${h}\","
        echo "\"arg\": \"${MDOC_HOME}/docs/$i\""
        echo "}"
    done
    echo "]}"
}

# 最后一个字符不是空格，且最后一次输入归类不是0，这表示该类型参数没有输入完成 --> 可以输出该类型选项
if [ "$1" = "${1% }" -a ${last_input} -gt 0 ];then
    end_option=0
# 下一个参数类型=最后一次输入类型，且类型不是0，表示该类型参数没有输入完成 --> 可以输出该类型选项
elif [ ${next_input} -gt 0 -a ${last_input} = ${next_input} ];then
    end_option=0
else
    end_option=1
fi
#echo "end_option=${end_option}"

## 如果当前输入为 tag，且没有结束输入，输出tag列表: 排除已经输入的tag，以当前输入为前缀的tag
if [ ${last_input} -eq 1 -a ${end_option} -eq 0 ]; then
    autocomplete=''
    n=${#tag_arr[@]}
    if [ ${n} -eq 0 ];then # 还没有输入任何值
        autocomplete="$1 "
        sql="select name from tag";
    elif [ "${end_char}" = "," ];then # 已输入若干个值，且最后一个值已确定
        autocomplete="$1"
        sql="select name from tag where 1=1";
        for i in ${tag_arr[@]}
        do
            sql="${sql} and name not like '${i}'"
        done
    else       # 已输入若干个值，且最后一个值还没有输入完成
        n=$((n-1))
        autocomplete="${1%${tag_arr[${n}]}}"
        sql="select name from tag where name like '${tag_arr[@]:$n}%'";
        for i in ${tag_arr[@]:0:${n}}
        do
            sql="${sql} and name not like '${i}'"
        done
    fi
#    echo ${sql}
    final_expr="sqlite3 \"${MDOC_HOME}/mainlib.db\" \"${sql}\""
    filtered_tags=`eval "${final_expr}"`
#    echo ${filtered_tags}
    output_tags "${autocomplete}"
    exit
fi

## 如果有输入-t参数，过滤文档tag
## 查询文档SQL:
##SELECT a.aid FROM tag_article a,article b
##WHERE a.aid = b.uuid AND
##  a.rid IN (SELECT id from tag b WHERE b.name LIKE 'TODO' or b.name LIKE 'DONE' )
##GROUP BY a.aid HAVING count(1) >=2
##ORDER BY b.dateModif DESC;
if [ ${#tag_arr[@]} -gt 0 ];then
    sql='select id from tag where '
    or_str=""
    for i in ${tag_arr[@]}
    do
        sql="${sql} ${or_str} name like '${i}'"
        or_str=or
    done
    sql="SELECT a.aid||'.md' FROM tag_article a,article b \
    WHERE a.aid=b.uuid AND a.rid IN (${sql}) \
    GROUP BY a.aid HAVING count(1)>=${#tag_arr[@]} \
    ORDER BY b.dateModif desc";
#    echo ${sql}
    final_expr="sqlite3 \"${MDOC_HOME}/mainlib.db\" \"${sql}\""
else
# 隐藏掉hide标签的文章
# select uuid || '.md' 
# from article 
# where uuid not in 
# (
#   SELECT a.aid FROM tag_article a,article b
#   WHERE a.aid = b.uuid 
#   AND a.rid IN (SELECT id from tag b WHERE b.name LIKE 'hide')
# ) 
# order by dateModif desc
    final_expr="sqlite3 \"${MDOC_HOME}/mainlib.db\" \"select uuid || '.md' from article where uuid not in (SELECT a.aid FROM tag_article a,article b WHERE a.aid = b.uuid AND a.rid IN (SELECT id from tag b WHERE b.name LIKE 'hide')) order by dateModif desc\""
fi

# 如果有输入-h参数，过滤文档标题
# 思路: grep -n 会输出行号，找到符合所有关键字的行，筛选行号=1的文档就可以了
if [ ${#header_arr[@]} -gt 0 ];then
    final_expr="${final_expr} | xargs grep -inHe '${header_arr[0]}'"
    for i in ${header_arr[@]:1}
    do
        final_expr="${final_expr} | grep -ie '${i}'"
    done
    final_expr="${final_expr} | egrep '^.+\.md\:1\:' | awk -F':' '{print \$1}'"
fi

# 如果有输入关键字，则用关键字筛选文档，并且按照文档标题匹配度排序
if [ ${#keyword_arr[@]} -gt 0 ];then
	
	#对于不含—t和-h 的查询进行性能优化
	if [ ${#tag_arr[@]} -eq 0 ] && [ ${#header_arr[@]} -eq 0 ];then
		
		keyword_str=""
		for i in ${keyword_arr[@]}
		do
			if [[ "$i" =~ ^[a-zA-Z0-9_\\-]+$ ]]
			then
				keyword_str="${keyword_str} && kMDItemTextContent == \"${i}\"c"
			else
				keyword_str="${keyword_str} && kMDItemTextContent == \"*${i}*\""
			fi
			
		done
		
		final_expr="mdfind -onlyin . 'kMDItemContentType == \"net.daringfireball.markdown\" ${keyword_str}'"
		
#		#convert absolute path to relative path
		IFS="/"
		final_expr="${final_expr} | sed -e 's/\/.*\///g'"
		unset IFS
	else
    		for i in ${keyword_arr[@]}
    		do
        		final_expr="${final_expr}| xargs grep -ile '${i}' | uniq "
    		done
	fi

    # 排序表达式: 统计第一行匹配关键字个数，将匹配个数大的放在前面
    # 第一步: 输入"文件名",输出"文件名 匹配个数"
    # 第二步: 按照 "匹配个数" 倒序排序
    # 第三步: 去掉 "匹配个数" 字段，只保留"文件名"
    # 由于ls -lt 是按照编辑时间倒序排序的，所以最终排序等级：标题匹配个数倒序->最后编辑倒序
    egrep_expr="$(echo "${keyword_arr[@]}" | sed "s/[[:blank:]]/|/g")"
    sort_expr="awk '{system(\"egrep -ioe \\\"${egrep_expr}\\\" <<< \`head -1 \"\$1 \"\`|wc -l | xargs echo \"\$1)}' | sort -rk 2 | awk '{print \$1}'"
    final_expr="${final_expr} | ${sort_expr} "
fi

final_expr="${final_expr} | head -20 " # 限制最多输出20条记录
#echo "$final_expr"
files=`eval "${final_expr}"`
output_files