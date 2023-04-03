package com.example.demo;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.example.demo.demo.MyersDiff;
import com.example.demo.demo.PathNode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
class DemoSpringboot3demoApplicationTests {


    @Test
    void contextLoads() throws IOException, DocumentException {
        File file = new ClassPathResource("static/ls.xml").getFile();

        Map<String, String> context = parseXml(file);

        System.out.println(JSON.toJSONString(context, JSONWriter.Feature.PrettyFormat));
    }

    public static final String INDEX_SUFFIX_PATTERN = "\\[\\d+]$";
    public static final String INDEX_SUFFIX_REPLACE = "[*]";

    /**
     * 解析XML为配置项key-value化
     * 案例:
     * 1. 简单节点解析 <system>eamp</system> 解析为map<string,string>对象 {/system: eamp}
     * 2. 属性节点解析 <os name="win"></os> 解析为map<string,string>对象 {/os/@name: win}
     * 3. 数组节点解析 <os><system>eamp</system><system>ls</system></os> 解析为map<string,string>对象 {/os/system[*]: [eamp,ls]}
     * 4. 父级节点有数组解析 <config><os><system>eamp</system><system>ls</system></os><os><system>win</system></os></config> 解析为map<string,string>对象 {/os[1]/system[*]: [eamp,ls]}
     *
     * @param xmlFile xml文件
     * @return Map<节点路径, 节点值>
     */

    private Map<String, String> parseXml(@NonNull File xmlFile) {
        Map<String, String> context = new HashMap<>();
        try {
            Document doc = new SAXReader().read(xmlFile);
            treeWalk(doc.getRootElement(), context);
        } catch (DocumentException e) {
            log.error("parse xml error", e);
        }
        // 合并数组JSON化
        return context.entrySet().stream()
            .collect(Collectors.groupingBy(entry -> entry.getKey().replaceAll(INDEX_SUFFIX_PATTERN, INDEX_SUFFIX_REPLACE)))
            .entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        if (entry.getValue().size() > 1) {
                            return JSON.toJSONString(entry.getValue().stream().map(Map.Entry::getValue).collect(Collectors.toList()));
                        } else {
                            return entry.getValue().get(0).getValue();
                        }
                    }
                )
            );
    }

    public void treeWalk(@NonNull Element element, @NonNull Map<String, String> context) {
        // 节点值
        String eleValue = element.getTextTrim();
        if (StringUtils.hasLength(eleValue)) {
            context.put(element.getUniquePath(), eleValue);
        }

        // 节点属性
        for (Iterator<Attribute> it = element.attributeIterator(); it.hasNext(); ) {
            Attribute attr = it.next();
            String attrValue = attr.getText().trim();
            if (StringUtils.hasLength(attrValue)) {
                context.put(attr.getUniquePath(), attrValue);
            }
        }

        // 子节点递归
        for (Iterator<Element> it = element.elementIterator(); it.hasNext(); ) {
            Element ele = it.next();
            treeWalk(ele, context);
        }
    }


    // https://chenshinan.github.io/2019/05/02/git%E7%94%9F%E6%88%90diff%E5%8E%9F%E7%90%86%EF%BC%9AMyers%E5%B7%AE%E5%88%86%E7%AE%97%E6%B3%95/
    public static void main(String[] args) throws IOException {

        List<String> oldList = Files.readAllLines(Path.of("D:\\workspace\\deaProjects\\demo-springboot3demo\\src\\main\\resources\\static\\old.json"), StandardCharsets.UTF_8);
        List<String> newList = Files.readAllLines(Path.of("D:\\workspace\\deaProjects\\demo-springboot3demo\\src\\main\\resources\\templates\\new.json"), StandardCharsets.UTF_8);

        MyersDiff<String> myersDiff = new MyersDiff<>();
        try {
            PathNode pathNode = myersDiff.buildPath(oldList, newList);
            System.out.println(pathNode);
            myersDiff.buildDiff(pathNode, oldList, newList);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
