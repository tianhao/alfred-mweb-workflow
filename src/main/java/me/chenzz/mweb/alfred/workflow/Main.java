package me.chenzz.mweb.alfred.workflow;

import java.io.IOException;
import java.util.Stack;
import me.chenzz.mweb.alfred.workflow.utils.DateFormatUtils;
import me.chenzz.mweb.alfred.workflow.utils.FileUtils;
import me.chenzz.mweb.alfred.workflow.utils.StringUtils;


public class Main {

    /**
     * 用来标识当前是否在code区域
     */
    private static boolean codeArea = false;

    public static void main(String[] args) throws IOException {

        String pathPrefix = "/Users/chenzz/Library/Containers/com.coderforart.MWeb3/Data/Library/Application Support/MWebLibrary/docs";
        String fileName = "15051376193683";
        String filePath = pathPrefix + "/" + fileName + ".md";
        if (args.length >= 1 && !StringUtils.isEmpty(args[0])) {
            filePath = args[0];
        }

        boolean topLeveTitleIncrease = true;
        if (args.length >= 2 && !StringUtils.isEmpty(args[1])) {
            try {
                topLeveTitleIncrease = Boolean.parseBoolean(args[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("filePath=" + filePath);


        String content = FileUtils.readFileToString(filePath, "UTF-8");

        // 先备份一下
        String backupFileName = DateFormatUtils.format(System.currentTimeMillis(), "yyyy年MM月dd日 HH时mm分ss秒");
        String path = "/tmp/" + backupFileName + ".md";
        FileUtils.writeStringToFile(path, content, "UTF-8");


        String[] lineArr = content.split("\n");
        Integer currentTitleLevel = null;



        Stack<TitleInfo> titleNoStack = new Stack<>();

        Integer topLevelWellNum = getTopLevelTitleWellNum(lineArr);
        Integer topLevelNum = getTopLevelTitleNum(lineArr, topLevelWellNum);

        int currentNo = 0;
        if (!topLeveTitleIncrease) {
            currentNo = topLevelNum + 1;
        }

        for (int i = 0; i < lineArr.length; i++) {
            String line = lineArr[i];

            if (!isTitle(line)) {
                continue;
            }

            int titleLevel = getTitleLevel(line);
            if (null == currentTitleLevel) {
                currentTitleLevel = titleLevel;
            }

            if (titleLevel == currentTitleLevel) {
                // 如果本行层级 和 当前的层级一致

                if (needDecrease(topLeveTitleIncrease, currentTitleLevel, topLevelWellNum)) {
                    currentNo--;
                } else {
                    currentNo++;
                }

                String newLine = generateNewLine(currentNo, titleNoStack, line);
                lineArr[i] = newLine;
            } else if (titleLevel > currentTitleLevel) {
                // 如果本行层级 小于 当前的层级 (2级标题 小于 1级标题)

                TitleInfo titleInfo = new TitleInfo(currentNo, currentTitleLevel);

                titleNoStack.push(titleInfo);
                currentTitleLevel = titleLevel;
                currentNo = 0;

                currentNo++;
                String newLine = generateNewLine(currentNo, titleNoStack, line);
                lineArr[i] = newLine;
            } else {
                // 如果本行层级 大于 当前的层级

                // 一直出栈到 本行层级相等的元素
                // （来应对那种跨级的情况，比如当前处在 4级标题下，下一行是2级标题，如果只出栈一次，则当前的层级还是3级）

                TitleInfo titleInfo = findEqualLevelEle(titleNoStack, titleLevel);
                if (null == titleInfo) {
                    throw new RuntimeException("无法找到和当前行层级一致的历史行，解析终止！"
                            + "问题原因是，文章某个标题出现了比前面标题等级高的情况；解决方案，在文章前面加一个和问题行等级一样的标记。line=" + line);
                }

                currentNo = titleInfo.getTitleNo();
                currentTitleLevel = titleInfo.getTitleLevel();

                if (needDecrease(topLeveTitleIncrease, currentTitleLevel, topLevelWellNum)) {
                    currentNo--;
                } else {
                    currentNo++;
                }

                String newLine = generateNewLine(currentNo, titleNoStack, line);
                lineArr[i] = newLine;
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (String line : lineArr) {
            stringBuilder.append(line).append("\n");
        }

        content = stringBuilder.toString();

        FileUtils.writeStringToFile(filePath, content, "UTF-8");
    }

    private static boolean needDecrease(boolean topLeveTitleIncrease, Integer currentTitleLevel,
            Integer topLevelWellNum) {
        return !topLeveTitleIncrease && topLevelWellNum != null && currentTitleLevel == topLevelWellNum;
    }

    private static Integer getTopLevelTitleNum(String[] lineArr, Integer topLevelWellNum) {
        // 顶级标题的数量
        int topLevelTitleNum = 0;

        for (int i = 0; i < lineArr.length; i++) {
            String line = lineArr[i];

            if (!isTitle(line)) {
                continue;
            }

            int wellNum = calWellNum(line);
            if (topLevelWellNum != null && wellNum == topLevelWellNum) {
                topLevelTitleNum++;
            }
        }

        return topLevelTitleNum;
    }

    private static Integer getTopLevelTitleWellNum(String[] lineArr) {
        // 顶级标题的井号数量
        Integer topLevelWellNum = null;
        for (int i = 0; i < lineArr.length; i++) {
            String line = lineArr[i];

            if (!isTitle(line)) {
                continue;
            }

            int wellNum = calWellNum(line);
            if (topLevelWellNum == null || wellNum < topLevelWellNum) {
                topLevelWellNum = wellNum;
            }
        }

        return topLevelWellNum;
    }

    private static int calWellNum(String line) {
        int num = 0;

        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '#') {
                num++;
            }
        }

        return num;
    }

    private static TitleInfo findEqualLevelEle(Stack<TitleInfo> titleNoStack, int titleLevel) {
        TitleInfo titleInfo = null;

        while (!titleNoStack.isEmpty()) {
            TitleInfo tempTitleInfo= titleNoStack.pop();
            if (!tempTitleInfo.getTitleLevel().equals(titleLevel)) {
                continue;
            } else {
                titleInfo = tempTitleInfo;
                break;
            }
        }
        return titleInfo;
    }

    private static String generateNewLine(int currentNo, Stack<TitleInfo> titleNoStack, String line) {
        String currentTitlePrefix = generateTitlePrefix(line, titleNoStack, currentNo);
        String newLine = currentTitlePrefix + extractTitleContent(line);

        System.out.println("newLine=" + newLine);

        return newLine;
    }

    private static String generateTitlePrefix(String line, Stack<TitleInfo> titleNoStack, int currentNo) {
        if (!line.startsWith("#")) {
            throw new RuntimeException("当前行不是#开头，无法解析出#前缀！line=" + line);
        }

        Integer i;
        for (i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '#') {
                continue;
            } else {
                break;
            }
        }
        String hashSignPrefix = line.substring(0, i);

        StringBuilder prefixStringBuilder = new StringBuilder();
        prefixStringBuilder.append(hashSignPrefix);
        prefixStringBuilder.append(" ");

        for (TitleInfo titleInfo : titleNoStack) {
            prefixStringBuilder.append(titleInfo.getTitleNo()).append(".");
        }
        prefixStringBuilder.append(currentNo).append(".");
        prefixStringBuilder.append(" ");

        return prefixStringBuilder.toString();
    }

    private static boolean isTitle(String line) {
        if (StringUtils.isEmpty(line)) {
            return false;
        }

        // 进出code区域
        if (line.contains("```")) {
            codeArea = !codeArea;
        }

        if (codeArea) {
            return false;
        }

        if (line.startsWith("# ")
                || line.startsWith("## ")
                || line.startsWith("### ")
                || line.startsWith("#### ")
                || line.startsWith("##### ")
                || line.startsWith("###### ")) {
            return true;
        } else {
            return false;
        }
    }

    private static int getTitleLevel(String line) {
        if (StringUtils.isEmpty(line)) {
            throw new RuntimeException("当前行不是title，无法获取标题层级！line=" + line);
        }
        if (line.startsWith("# ")) {
            return 1;
        } else if (line.startsWith("## ")) {
            return 2;
        } else if (line.startsWith("### ")) {
            return 3;
        } else if (line.startsWith("#### ")) {
            return 4;
        } else if (line.startsWith("##### ")) {
            return 5;
        } else if (line.startsWith("###### ")) {
            return 6;
        } else {
            throw new RuntimeException("当前行无法获取标题层级！line=" + line);
        }
    }

    /**
     * 抽取标题内容
     *
     * 例如，
     *
     * ### 1.2.3. 旁路缓存
     * 对应的标题内容是 旁路缓存
     *
     * @param line
     * @return
     */
    private static String extractTitleContent(String line) {
        if (StringUtils.isEmpty(line)) {
            throw new RuntimeException("当前行不是title，无法获取标题内容！line=" + line);
        }

        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);
            if ('#'!= currentChar
                    && ' ' != currentChar
                    && (currentChar < '0' || currentChar > '9')
                    && '.' != currentChar
                    && '、' != currentChar
                    && '）' != currentChar
                    && ')' != currentChar) {
                String lineContent = line.substring(i);
                return lineContent;
            } else {
                continue;
            }
        }
        return "";
    }

}
