/*
 * souche.com
 * Copyright (C) 2013-2019 All Rights Reserved.
 */
package com.uifuture.maven.plugins.util;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaPackage;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaSource;
import com.thoughtworks.qdox.model.JavaType;
import com.uifuture.maven.plugins.dto.JavaClassDTO;
import com.uifuture.maven.plugins.dto.JavaExceptionsDTO;
import com.uifuture.maven.plugins.dto.JavaImplementsDTO;
import com.uifuture.maven.plugins.dto.JavaMethodDTO;
import com.uifuture.maven.plugins.dto.JavaParameterDTO;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author chenhx
 * @version JavaProjectBuilderUtil.java, v 0.1 2019-06-10 15:58 chenhx
 */
public class JavaProjectBuilderUtil {
    private static Log log = new SystemStreamLog();

    public static String baseDir;

    /**
     * 对应类型转换
     */
    private static final Map<String, String> MAPPING = new HashMap<>();

    /**
     * 对应类型的默认值
     */
    private static final Map<String, String> VALUE = new HashMap<>();
    static {
        //初始化类型转换
        initMapping();
        //初始化默认值
        initValue();
    }

    private static void initMapping() {
        MAPPING.put("java.lang.Integer", "Integer");
        MAPPING.put("java.lang.Long", "Long");
        MAPPING.put("java.lang.Double", "Double");
        MAPPING.put("java.lang.String", "String");
        MAPPING.put("java.lang.Boolean", "Boolean");
        MAPPING.put("java.lang.Byte", "Byte");
        MAPPING.put("java.lang.Float", "Float");
        MAPPING.put("java.lang.Object", "Object");
        MAPPING.put("java.lang.Short", "Short");
        MAPPING.put("java.lang.StringBuffer", "StringBuffer");
        MAPPING.put("java.lang.StringBuilder", "StringBuilder");
        MAPPING.put("java.lang.Void", "Void");

        MAPPING.put("java.util.Map", "java.util.HashMap");
        MAPPING.put("java.util.List", "java.util.ArrayList");
        MAPPING.put("java.util.set", "java.util.HashSet");
        MAPPING.put("T", "Object");
        MAPPING.put("B", "Object");
        MAPPING.put("M", "Object");
        MAPPING.put("F", "Object");
    }
    private static void initValue() {
        VALUE.put("String","\"\"");
        VALUE.put("Integer","0");
        VALUE.put("Long","0L");
        VALUE.put("Double","0.0");
        VALUE.put("Float","0.0f");
        VALUE.put("Boolean","true");

        VALUE.put("int","0");
        VALUE.put("long","0L");
        VALUE.put("double","0.0");
        VALUE.put("float","0.0f");
        VALUE.put("boolean","true");
        VALUE.put("char","'0'");
        VALUE.put("byte","0");
        VALUE.put("short","0");

        VALUE.put("StringBuffer","new StringBuffer(\"\")");
        VALUE.put("StringBuilder","new StringBuilder(\"\")");

        VALUE.put("java.util.HashMap","new java.util.HashMap()");
        VALUE.put("java.util.ArrayList","new java.util.ArrayList()");
        VALUE.put("java.util.HashSet","new java.util.HashSet()");
        VALUE.put("java.util.Date","new java.util.Date()");
    }

    public static void main(String[] args) throws IOException {
        String javaName = "/Users/chenhx/Desktop/github/auto-unit-test-plugin/src/main/java/com/uifuture/maven/plugins/BuilderUtilsTest.java";
        String mainJava="/Users/chenhx/Desktop/github/auto-unit-test-plugin/src/main/java/";

        String name = "com.uifuture.maven.plugins.BuilderUtilsTest";
        buildTestMethod(javaName, name, mainJava);
    }

    /**
     * 生成测试方法
     *  @param javaNameFile java文件的绝对路径
     * @param javaName     java类的全限定名称
     * @param mainJava 项目中java类的跟路径
     */
    public static JavaClassDTO buildTestMethod(String javaNameFile, String javaName, String mainJava) throws IOException {

        JavaClassDTO javaClassDTO = new JavaClassDTO();
        /*
         * 获取类库
         */
        JavaProjectBuilder builder = new JavaProjectBuilder();
        // 获取类库
//        JavaProjectBuilder builder = new JavaProjectBuilder();
        // 正在读取单个源文件
//        builder.addSource(new File(javaNameFile));
        //读取包下所有的java类文件
        builder.addSourceTree(new File(mainJava));

        //获取Java类
        JavaClass cls = builder.getClassByName(javaName);
        log.info("正在构建类：" + cls);
        if (cls.isInterface()) {
            log.info("跳过接口：" + cls);
            return null;
        }
        if (cls.isEnum()) {
            log.info("跳过枚举：" + cls);
            return null;
        }
        if (cls.isAbstract()) {
            log.info("跳过抽象类：" + cls);
            return null;
        }
        if (cls.isPrivate()) {
            log.info("跳过私有类：" + cls);
            return null;
        }

        //获取导入的包
        List<JavaImplementsDTO> javaImplementsDTOList = getJavaImplementsDTOList(cls);
        javaClassDTO.setJavaImplementsDTOList(javaImplementsDTOList);

        //设置包名
        JavaPackage pkg = cls.getPackage();
        javaClassDTO.setPackageName(pkg.getName());

        List<JavaMethodDTO> javaMethodDTOList = new ArrayList<>();
        //获取方法集合
        List<JavaMethod> javaMethodList = cls.getMethods();

        Map<String,Integer> methodMap = new HashMap<>();

        Map<String,List<JavaParameterDTO>> javaParameterDTOMap = new HashMap<>();
        for (JavaMethod javaMethod : javaMethodList) {
            JavaMethodDTO javaMethodDTO = new JavaMethodDTO();
            //获取方法名称
            String methodName = javaMethod.getName();
            javaMethodDTO.setMethodName(methodName);
            javaMethodDTO.setMethodTestName(methodName);
            if(methodMap.containsKey(methodName)){
                Integer t = methodMap.get(methodName);
                javaMethodDTO.setMethodTestName(methodName+t);
                methodMap.put(methodName,++t);
            }else {
                methodMap.put(methodName,1);
            }

            //获取方法返回类型
            JavaClass returnValue = javaMethod.getReturns();
            String returnValueStr = returnValue.toString();
            returnValueStr = MAPPING.getOrDefault(returnValueStr,returnValueStr);
            javaMethodDTO.setMethodReturnType(returnValueStr);

            //是否是静态方法
            boolean mStatic = javaMethod.isStatic();
            if (mStatic) {
                continue;
            }
            //是否公共方法
            boolean mPublic = javaMethod.isPublic();
            if (!mPublic) {
                continue;
            }
            //参数
            List<JavaParameterDTO> javaParameterDTOS = getJavaParameterDTOList(javaMethod,javaParameterDTOMap,builder);
            javaMethodDTO.setJavaParameterDTOList(javaParameterDTOS);

            //方法抛出的异常
            List<JavaExceptionsDTO> javaExceptionsDTOS = getJavaExceptionsDTOList(javaMethod);
            javaMethodDTO.setJavaExceptionsDTOList(javaExceptionsDTOS);

            //返回的是否是数组
            boolean mArray = javaMethod.getReturns().isArray();
            javaMethodDTOList.add(javaMethodDTO);
        }
        javaClassDTO.setJavaMethodDTOList(javaMethodDTOList);
        javaClassDTO.setJavaParameterDTOMap(javaParameterDTOMap);
        log.info("构建的类信息：" + javaClassDTO);
        return javaClassDTO;
    }

    /**
     * 方法抛出的异常
     *
     * @param javaMethod
     * @return
     */
    private static List<JavaExceptionsDTO> getJavaExceptionsDTOList(JavaMethod javaMethod) {
        List<JavaClass> exceptions = javaMethod.getExceptions();
        List<JavaExceptionsDTO> javaExceptionsDTOS = new ArrayList<>();
        for (JavaClass exception : exceptions) {
            JavaExceptionsDTO javaExceptionsDTO = new JavaExceptionsDTO();
            javaExceptionsDTO.setType(exception.toString());
            javaExceptionsDTOS.add(javaExceptionsDTO);
        }
        return javaExceptionsDTOS;
    }

    /**
     * 获取的参数
     *
     * @param javaMethod
     * @param javaParameterDTOMap
     * @param builder
     * @return
     */
    private static List<JavaParameterDTO> getJavaParameterDTOList(JavaMethod javaMethod, Map<String, List<JavaParameterDTO>> javaParameterDTOMap, JavaProjectBuilder builder) {
        List<JavaParameter> parameterList = javaMethod.getParameters();
        List<JavaParameterDTO> javaParameterDTOS = new ArrayList<>();
        //TODO 暂时未处理其他泛型,简单的使用Obj代替
        for (JavaParameter javaParameter : parameterList) {
            JavaParameterDTO javaParameterDTO = new JavaParameterDTO();
            javaParameterDTO.setName(javaParameter.getName());
            String typeToStr = javaParameter.getType().toString();
            String type = MAPPING.getOrDefault(typeToStr, typeToStr);
            javaParameterDTO.setType(type);
            javaParameterDTO.setCustomType(false);
            //设置默认值
            javaParameterDTO.setValue(VALUE.getOrDefault(type,null));

            String keyName = UUIDUtil.getID();
            javaParameterDTO.setKeyName(keyName);
            if(type.equals(typeToStr)){
                //自定义类型 暂时处理一层包装类
                JavaClass cls = builder.getClassByName(typeToStr);
                log.info("自定义类型："+typeToStr+"，cls："+cls);
                if(cls!=null){
                    javaParameterDTO.setCustomType(true);
                    List<JavaParameterDTO> javaParameterDTOList = new ArrayList<>();
                    //获取属性
                    addParameterToList(javaParameterDTOList, cls);

                    //获取父类属性 - 暂时也只获取一层
                    JavaClass superJavaClass = cls.getSuperJavaClass();
                    if(!MAPPING.containsKey(superJavaClass.toString())){
                        addParameterToList(javaParameterDTOList, superJavaClass);
                    }

                    log.info("superJavaClass:"+superJavaClass);

                    log.info("自定义类型"+typeToStr+"，自定义类型中的类型："+javaParameterDTOList);
                    javaParameterDTOMap.put(keyName,javaParameterDTOList);
                }
            }
            javaParameterDTOS.add(javaParameterDTO);
        }
        return javaParameterDTOS;
    }

    /**
     * 获取属性，设置到对象中去
     * @param javaParameterDTOList
     * @param superJavaClass
     */
    private static void addParameterToList(List<JavaParameterDTO> javaParameterDTOList, JavaClass superJavaClass) {
        List<JavaField> javaFields = superJavaClass.getFields();
        if (javaFields.isEmpty()) {
            log.warn("获取的类下的属性为空，可能是由于不在同一个项目，类：" + superJavaClass);
        }
        for (JavaField javaField : javaFields) {
            if (javaField.isStatic()) {
                continue;
            }
            if (javaField.isFinal()) {
                continue;
            }
            //遍历属性,属性名称
            String fieldName = javaField.getName();
            //获取属性类型
            String fieldTypeStr = javaField.getType().toString();
            String fieldType = MAPPING.getOrDefault(fieldTypeStr, fieldTypeStr);
            JavaParameterDTO javaParameterDTO1 = new JavaParameterDTO();
            javaParameterDTO1.setKeyName("");
            javaParameterDTO1.setCustomType(false);
            javaParameterDTO1.setName(fieldName);
            javaParameterDTO1.setType(fieldType);
            javaParameterDTO1.setValue(VALUE.getOrDefault(fieldType, null));
            javaParameterDTO1.setUpName(StringUtil.strConvertUpperCamel(fieldName));
            javaParameterDTOList.add(javaParameterDTO1);
        }
    }

    /**
     * 获取导入的包名
     *
     * @param cls
     * @return
     */
    private static List<JavaImplementsDTO> getJavaImplementsDTOList(JavaClass cls) {
        List<JavaType> javaTypeList = cls.getImplements();
        List<JavaImplementsDTO> javaImplementsDTOList = new ArrayList<>();
        for (JavaType javaType : javaTypeList) {
            JavaImplementsDTO javaImplementsDTO = new JavaImplementsDTO();
            javaImplementsDTO.setType(javaType.toString());
            javaImplementsDTOList.add(javaImplementsDTO);
        }
        return javaImplementsDTOList;
    }
}