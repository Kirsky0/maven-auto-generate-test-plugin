/*
 * souche.com
 * Copyright (C) 2013-2019 All Rights Reserved.
 */
package com.uifuture.maven.plugins.util;

import com.uifuture.maven.plugins.common.BaseConstant;
import com.uifuture.maven.plugins.common.InitConstant;
import com.uifuture.maven.plugins.dto.JavaMockClassInfoDTO;
import com.uifuture.maven.plugins.dto.JavaMockMethodInfoDTO;
import com.uifuture.maven.plugins.model.JavaMethodModel;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaPackage;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaType;
import com.uifuture.maven.plugins.dto.JavaClassDTO;
import com.uifuture.maven.plugins.dto.JavaExceptionsDTO;
import com.uifuture.maven.plugins.dto.JavaImplementsDTO;
import com.uifuture.maven.plugins.dto.JavaMethodDTO;
import com.uifuture.maven.plugins.dto.JavaParameterDTO;
import com.uifuture.maven.plugins.model.JavaClassModel;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author chenhx
 * @version JavaProjectBuilderUtil.java, v 0.1 2019-06-10 15:58 chenhx
 */
public class JavaProjectBuilderUtil {
    private static Log log = new SystemStreamLog();

    public static String baseDir;

    /**
     * 需要跳过的包的类，不进行设置默认值
     */
    private static final Set<String> SKIP_PACKAGE = new HashSet<>();

    /**
     * Getter method for property <tt>SKIP_PACKAGE</tt>.
     *
     * @return property value of SKIP_PACKAGE
     */
    public static Set<String> getSkipPackage() {
        return SKIP_PACKAGE;
    }

    /**
     * 获取类库
     */
    private static JavaProjectBuilder builder = new JavaProjectBuilder();


    public static JavaProjectBuilder getBuilder() {
        return builder;
    }


    public static void main(String[] args) throws IOException {
        String javaName = "/Users/chenhx/Desktop/github/auto-unit-test-plugin/src/main/java/com/uifuture/maven/plugins/BuilderUtilsTest.java";
        String mainJava = "/Users/chenhx/Desktop/github/auto-unit-test-plugin/src/main/java/";

        String name = "com.uifuture.maven.plugins.BuilderUtilsTest";
        builder.addSourceTree(new File(mainJava));
        buildTestMethod(javaName, name);
    }

    /**
     * 生成测试方法
     *
     * @param javaNameFile java文件的绝对路径
     * @param javaName     java类的全限定名称
     */
    public static JavaClassDTO buildTestMethod(String javaNameFile, String javaName) throws IOException {

        /*
         * 需要导入的包，string-类简称，value-全称限定类名的，如果有多个，后面的使用全限定名
         */
        Map<String, String> implementsJavaPackageMap = new HashMap<>();

        JavaClassDTO javaClassDTO = new JavaClassDTO();

        //获取Java类
        JavaClass javaClass = builder.getClassByName(javaName);
        log.info("正在构建类：" + javaClass);
        if (javaClass == null) {
            log.error("未查询到该类，请确保项目包中有该类，类名：" + javaName);
            return null;
        }

        if (javaClass.isInterface()) {
            log.info("跳过接口：" + javaClass);
            return null;
        }
        if (javaClass.isEnum()) {
            log.info("跳过枚举：" + javaClass);
            return null;
        }
        if (javaClass.isAbstract()) {
            log.info("跳过抽象类：" + javaClass);
            return null;
        }
        if (javaClass.isPrivate()) {
            log.info("跳过私有类：" + javaClass);
            return null;
        }


        //获取类的属性
        List<JavaField> javaFieldList = javaClass.getFields();
        //TODO 暂时未考虑重名方法！！！
        Map<String, JavaClassModel> mockJavaClassModelMap = new HashMap<>();
        //需要mock的类
        List<JavaMockClassInfoDTO> javaMockClassInfoDTOList = new ArrayList<>();
        //获取mock的信息
        getMockClass(javaClass, javaMockClassInfoDTOList, javaFieldList, mockJavaClassModelMap);

        //设置包名
        JavaPackage pkg = javaClass.getPackage();
        javaClassDTO.setPackageName(pkg.getName());

        List<JavaMethodDTO> javaMethodDTOList = new ArrayList<>();
        //获取方法集合
        List<JavaMethod> javaMethodList = javaClass.getMethods();

        //构建类
        Map<String, Integer> methodMap = new HashMap<>();
        //包装类的内部属性 - 包含了父类的属性
        Map<String, List<JavaParameterDTO>> javaParameterDTOMap = new HashMap<>();
        buildClass(mockJavaClassModelMap, javaMethodDTOList, javaMethodList, methodMap, javaParameterDTOMap,implementsJavaPackageMap);

        //获取导入的包
        List<JavaImplementsDTO> javaImplementsDTOList = new ArrayList<>();
        //全称限定名称
        for (String key : implementsJavaPackageMap.keySet()) {
            JavaImplementsDTO javaImplementsDTO = new JavaImplementsDTO();
            String type = implementsJavaPackageMap.get(key);
            if(InitConstant.EXCLUDE_IMPORT_TYPE.contains(type)){
                continue;
            }
            //获取导入
            javaImplementsDTO.setType(type);
            javaImplementsDTOList.add(javaImplementsDTO);
        }
//        List<JavaImplementsDTO> javaImplementsDTOList = getJavaImplementsDTOList(javaClass);
        javaClassDTO.setJavaImplementsDTOList(javaImplementsDTOList);


        javaClassDTO.setJavaMethodDTOList(javaMethodDTOList);
        javaClassDTO.setJavaParameterDTOMap(javaParameterDTOMap);
        javaClassDTO.setJavaMockClassInfoDTOList(javaMockClassInfoDTOList);
        log.info("构建的类信息：" + javaClassDTO);
        return javaClassDTO;
    }


    /**
     * 核心方法
     *
     * @param mockJavaClassModelMap
     * @param javaMethodDTOList
     * @param javaMethodList
     * @param methodMap
     * @param javaParameterDTOMap 包装类的内部属性 - 包含了父类的属性
     */
    private static void buildClass(Map<String, JavaClassModel> mockJavaClassModelMap, List<JavaMethodDTO> javaMethodDTOList,
                                   List<JavaMethod> javaMethodList, Map<String, Integer> methodMap,
                                   Map<String, List<JavaParameterDTO>> javaParameterDTOMap,
                                    Map<String,String> implementsJavaPackageMap) {
        //遍历类中的方法
        for (JavaMethod javaMethod : javaMethodList) {
            Map<String, List<JavaMockMethodInfoDTO>> javaMockMethodInfoDTOMap = new HashMap<>();
            JavaMethodDTO javaMethodDTO = new JavaMethodDTO();
            //获取方法名称
            String methodName = javaMethod.getName();
            javaMethodDTO.setMethodName(methodName);
            javaMethodDTO.setMethodTestName(methodName);
            if (methodMap.containsKey(methodName)) {
                Integer t = methodMap.get(methodName);
                javaMethodDTO.setMethodTestName(methodName + t);
                methodMap.put(methodName, ++t);
            } else {
                methodMap.put(methodName, 1);
            }

            //获取方法返回类型
            JavaClass returnValue = javaMethod.getReturns();
            String returnValueStr = returnValue.getFullyQualifiedName();
            returnValueStr = InitConstant.MAPPING.getOrDefault(returnValueStr, returnValueStr);
            javaMethodDTO.setMethodReturnType(returnValueStr);

            if (excludeMethod(javaMethod)) {
                continue;
            }

            //方法参数的设置，包装类设置属性 默认值
            List<JavaParameterDTO> javaParameterDTOS = getJavaParameterDTOList(javaMethod, javaParameterDTOMap, builder);
            //处理全称限定名称 - 简称
            handleQualifiedName(javaParameterDTOS,implementsJavaPackageMap);


            javaMethodDTO.setJavaParameterDTOList(javaParameterDTOS);

            //方法抛出的异常
            List<JavaExceptionsDTO> javaExceptionsDTOS = getJavaExceptionsDTOList(javaMethod);
            javaMethodDTO.setJavaExceptionsDTOList(javaExceptionsDTOS);

            //Mock方法模拟
            List<JavaMockMethodInfoDTO> javaMockMethodInfoDTOList = new ArrayList<>();
            //获取方法的源码
            String methodCode = javaMethod.getSourceCode();

            Set<String> methodNameSet = new HashSet<>();
            //判断方法中是否有需要mock的方法
            for (String name : mockJavaClassModelMap.keySet()) {

                //name - 属性名称
                String pattern = name + "\\.\\w+\\([\\S ]+\\);";
                //正则匹配
                Pattern p = Pattern.compile(pattern);
                // 获取 matcher 对象
                Matcher m = p.matcher(methodCode);
                while (m.find()) {
                    saveMockMethodInfoDTO(mockJavaClassModelMap, javaMockMethodInfoDTOList, methodCode, methodNameSet, name, m);
                }

            }
            javaMockMethodInfoDTOMap.put(methodName, javaMockMethodInfoDTOList);
            javaMethodDTO.setJavaMockMethodInfoMap(javaMockMethodInfoDTOMap);
            javaMethodDTOList.add(javaMethodDTO);
        }
    }

    /**
     * 处理全称限定名称 - 简称
     * @param javaParameterDTOS
     */
    private static void handleQualifiedName(List<JavaParameterDTO> javaParameterDTOS,Map<String,String> implementsJavaPackageMap) {
        //处理全限定名称
        for (JavaParameterDTO javaParameterDTO : javaParameterDTOS) {
            String type = javaParameterDTO.getType();
            //获取类型 简称
            String abbType = type.substring(type.lastIndexOf(".")+1);
            if(implementsJavaPackageMap.containsKey(abbType)){
                String t = implementsJavaPackageMap.get(abbType);
                if(t.equals(type)){
                    javaParameterDTO.setType(abbType);
                }
            }else {
                implementsJavaPackageMap.put(abbType, type);
                javaParameterDTO.setType(abbType);
            }
        }
    }

    /**
     * 保存方法中对应使用的mock方法的一些信息
     *
     * @param mockJavaClassModelMap
     * @param javaMockMethodInfoDTOList
     * @param methodCode
     * @param methodNameSet
     * @param name
     * @param m
     */
    private static void saveMockMethodInfoDTO(Map<String, JavaClassModel> mockJavaClassModelMap,
                                              List<JavaMockMethodInfoDTO> javaMockMethodInfoDTOList,
                                              String methodCode, Set<String> methodNameSet, String name, Matcher m) {
        String str = methodCode.substring(m.start(), m.end());
        JavaMockMethodInfoDTO javaMockMethodInfoDTO = new JavaMockMethodInfoDTO();
        JavaClassModel javaClassModel = mockJavaClassModelMap.get(name);
        if (javaClassModel == null) {
            log.warn("获取的mock类数据为NULL，"
                    + "mockJavaClassModelMap=" + mockJavaClassModelMap
                    + ",name=" + name
                    + ",方法源码=" + methodCode);
            return;
        }
        javaMockMethodInfoDTO.setClassType(javaClassModel.getType());
        javaMockMethodInfoDTO.setClassName(name);
        //TODO 方法名称 - 这里实际还需要区分参数类型和参数个数，否则无法匹配到唯一的方法,目前不支持重名方法！！！
        String nameS = str.substring(str.indexOf(".") + 1, str.indexOf("("));
        javaMockMethodInfoDTO.setName(nameS);
        log.info("获取到Mock的方法："+str+",nameS="+nameS);

        int num = str.split(",").length;
        //判断是否是空参方法
        if(str.contains(nameS+"();")){
            num=0;
        }

        JavaMethodModel javaMethodModel = null;

        List<JavaMethodModel> javaMethodModelList = javaClassModel.getJavaMethodModelList();
        for (JavaMethodModel methodModel : javaMethodModelList) {
            if (methodModel.getName().equals(nameS)) {
                javaMethodModel = methodModel;
                break;
            }
        }
        if (javaMethodModel == null) {
            javaMethodModel = getJavaMethodModelByParent(mockJavaClassModelMap, name, nameS, javaMethodModel);
            if (javaMethodModel == null) {
                //手动拼接
                javaMethodModel = new JavaMethodModel();
                javaMethodModel.setParameterNum(num);
                List<String> list = new ArrayList<>();
                for(int i=0;i<num;i++){
                    list.add(i+"");
                }
                javaMethodModel.setParameterName(list);
                javaMethodModel.setParameterType(list);
             }
        }
        //设置参数数量
        javaMockMethodInfoDTO.setParameterNum(javaMethodModel.getParameterNum());
        javaMockMethodInfoDTO.setParameterName(javaMethodModel.getParameterName());
        javaMockMethodInfoDTO.setParameterType(javaMethodModel.getParameterType());
        //去重
        if (methodNameSet.contains(nameS)) {
            return;
        }
        log.info("mock方法的属性："+javaMockMethodInfoDTO);
        javaMockMethodInfoDTOList.add(javaMockMethodInfoDTO);
        methodNameSet.add(nameS);
    }

    /**
     * 排除方法
     *
     * @param javaMethod
     * @return
     */
    private static boolean excludeMethod(JavaMethod javaMethod) {
        //是否是静态方法
        boolean mStatic = javaMethod.isStatic();
        if (mStatic) {
            return true;
        }
        //是否公共方法
        boolean mPublic = javaMethod.isPublic();
        return !mPublic;
    }

    /**
     * 通过父类进行获取方法的属性
     *
     * @param mockJavaClassModelMap
     * @param name
     * @param nameS
     * @param javaMethodModel
     * @return
     */
    private static JavaMethodModel getJavaMethodModelByParent(Map<String, JavaClassModel> mockJavaClassModelMap, String name, String nameS, JavaMethodModel javaMethodModel) {
        //通过父类再进行获取
        JavaClass javaClass = builder.getClassByName(name);
        if (javaClass == null) {
            log.warn("没有找到该类，类名："
                    + name + "，javaClass=null");
            return null;
        }
        JavaClass superJavaClass = javaClass.getSuperJavaClass();
        if (superJavaClass == null) {
            log.warn("没有找到该类的父类，类名："
                    + name + "，superJavaClass=null，javaClass=" + javaClass);
            return null;
        }

        JavaClassModel javaClassModel1 = BaseConstant.mockParentJavaClassModelMap.get(superJavaClass.getFullyQualifiedName());
        if (javaClassModel1 == null) {
            log.warn("没有找到该父类的JavaClassModel，superJavaClass：" + superJavaClass + "，javaClass："
                    + javaClass + "，BaseConstant.mockParentJavaClassModelMap=" + BaseConstant.mockParentJavaClassModelMap);
            return null;
        }
        for (JavaMethodModel methodModel : javaClassModel1.getJavaMethodModelList()) {
            //获取到对应的方法
            if (methodModel.getName().equals(nameS)) {
                javaMethodModel = methodModel;
                break;
            }
        }
        if (javaMethodModel == null) {
            log.warn("在类中没有找到该方法，方法名：" + nameS + "，类名："
                    + name + "，mockJavaClassModelMap=" + mockJavaClassModelMap + "，BaseConstant.mockParentJavaClassModelMap=" + BaseConstant.mockParentJavaClassModelMap);
            return null;
        }
        return javaMethodModel;
    }

    /**
     * 获取mock的信息，
     * 遍历类中属性，以及属性名称，设置到需要mock的类的信息
     *
     * @param javaClass
     * @param javaMockClassInfoDTOList
     * @param javaFieldList
     * @param mockJavaClassModelMap
     */
    private static void getMockClass(JavaClass javaClass, List<JavaMockClassInfoDTO> javaMockClassInfoDTOList,
                                     List<JavaField> javaFieldList, Map<String, JavaClassModel> mockJavaClassModelMap) {
        for (JavaField javaField : javaFieldList) {
            JavaMockClassInfoDTO javaMockClassInfoDTO = new JavaMockClassInfoDTO();

            if (javaField.isStatic()) {
                continue;
            }
            if (javaField.isFinal()) {
                continue;
            }
            JavaClassModel javaClassModel = new JavaClassModel();
            //属性名称
            javaClassModel.setName(javaField.getName());
            String typeStr = javaField.getType().getFullyQualifiedName();
            String type = InitConstant.MAPPING.getOrDefault(typeStr, typeStr);
            javaClassModel.setType(type);
            //获取类型中的方法 Java类

            //排除不需要mock的类
            if (!BaseConstant.mockJavaSet.contains(typeStr)) {
                continue;
            }

            List<JavaMethodModel> javaMethodModelList = new ArrayList<>();
            List<JavaMockMethodInfoDTO> javaMockMethodInfoDTOList = new ArrayList<>();

            //TODO 获取父类 - 暂时只支持两层 - 暂时也不支持其他jar包中的类
            JavaClass superClass = javaField.getType().getSuperJavaClass();
            if(superClass!=null) {
                log.info("获取的父类：" + superClass);
                //获取类中方法
                JavaClass fieldClass = builder.getClassByName(typeStr);
                List<JavaMethod> fieldMethodList = fieldClass.getMethods();
                for (JavaMethod javaMethod : fieldMethodList) {
                    setMockMethodInfo(javaField, type, javaMockMethodInfoDTOList, javaMethod, superClass);
                }

                List<JavaMethod> superJavaMethod = superClass.getMethods();
                if (!BaseConstant.mockParentJavaClassModelMap.containsKey(superClass.getFullyQualifiedName())) {
                    JavaClassModel javaClassModel1 = new JavaClassModel();
                    javaClassModel1.setName(superClass.getName());
                    javaClassModel1.setType(superClass.getFullyQualifiedName());
                    List<JavaMethodModel> javaMethodModelList1 = new ArrayList<>();
                    for (JavaMethod javaMethod : superJavaMethod) {
                        //存储父类信息
                        JavaMethodModel javaMethodModel = saveJavaMethodModel(javaMethod);
                        javaMethodModelList1.add(javaMethodModel);
                    }
                    javaClassModel1.setJavaMethodModelList(javaMethodModelList1);
                    BaseConstant.mockParentJavaClassModelMap.put(superClass.getFullyQualifiedName(), javaClassModel1);
                }
                javaMockClassInfoDTO.setParentType(superClass.getFullyQualifiedName());
            }

            javaMockClassInfoDTO.setName(javaField.getName());
            javaMockClassInfoDTO.setType(type);
            javaMockClassInfoDTO.setJavaMockMethodInfoDTOList(javaMockMethodInfoDTOList);
            //获取类型中的方法 Java类 - 排除不需要mock的java类
            if (BaseConstant.mockJavaSet.contains(type)) {
                //说明该属性类需要mock
                javaMockClassInfoDTOList.add(javaMockClassInfoDTO);
            }

            javaClassModel.setJavaMethodModelList(javaMethodModelList);
            mockJavaClassModelMap.put(javaClassModel.getName(), javaClassModel);

        }
        //属性的相关信息
        log.info("本类属性相关信息，类：" + javaClass.getName() + ", mock属性类相关信息：" + mockJavaClassModelMap);
    }

    private static JavaMethodModel saveJavaMethodModel(JavaMethod javaMethod) {
        JavaMethodModel javaMethodModel = new JavaMethodModel();
        javaMethodModel.setName(javaMethod.getName());
        //方法参数
        List<JavaParameter> javaParameterList = javaMethod.getParameters();
        List<String> parameterNameList = new ArrayList<>();
        List<String> parameterTypeList = new ArrayList<>();
        for (JavaParameter javaParameter : javaParameterList) {
            parameterNameList.add(javaParameter.getName());
            String typeS = javaParameter.getType().getFullyQualifiedName();
            String pType = InitConstant.MAPPING.getOrDefault(typeS, typeS);
            parameterTypeList.add(pType);
        }
        javaMethodModel.setParameterName(parameterNameList);
        javaMethodModel.setParameterType(parameterTypeList);
        javaMethodModel.setParameterNum(parameterNameList.size());
        String rTypeStr = javaMethod.getReturnType().getFullyQualifiedName();
        String rType = InitConstant.MAPPING.getOrDefault(rTypeStr, rTypeStr);
        javaMethodModel.setReturnType(rType);
        return javaMethodModel;
    }

    /**
     * 设置mock方法的信息
     *
     * @param javaField
     * @param type
     * @param javaMockMethodInfoDTOList
     * @param javaMethod
     * @param superClass
     */
    private static void setMockMethodInfo(JavaField javaField, String type, List<JavaMockMethodInfoDTO> javaMockMethodInfoDTOList, JavaMethod javaMethod, JavaClass superClass) {
        JavaMockMethodInfoDTO javaMockMethodInfoDTO = new JavaMockMethodInfoDTO();

        JavaMethodModel javaMethodModel = saveJavaMethodModel(javaMethod);

        javaMockMethodInfoDTO.setClassName(javaField.getName());
        javaMockMethodInfoDTO.setClassType(type);
        javaMockMethodInfoDTO.setName(javaMethod.getName());
        javaMockMethodInfoDTO.setParameterNum(javaMethodModel.getParameterNum());
        javaMockMethodInfoDTO.setParameterName(javaMethodModel.getParameterName());
        javaMockMethodInfoDTO.setParameterType(javaMethodModel.getParameterType());
        javaMockMethodInfoDTO.setReturnType(javaMethodModel.getReturnType());
        //设置父类类型
        javaMockMethodInfoDTO.setParentClassType(superClass.getFullyQualifiedName());
        javaMockMethodInfoDTOList.add(javaMockMethodInfoDTO);
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
            javaExceptionsDTO.setType(exception.getFullyQualifiedName());
            javaExceptionsDTOS.add(javaExceptionsDTO);
        }
        return javaExceptionsDTOS;
    }

    /**
     * 获取的参数
     * 参数组装-方法参数
     * @param javaMethod
     * @param javaParameterDTOMap
     * @param builder
     * @return
     */
    private static List<JavaParameterDTO> getJavaParameterDTOList(JavaMethod javaMethod,
                                                                  Map<String, List<JavaParameterDTO>> javaParameterDTOMap,
                                                                  JavaProjectBuilder builder) {
        List<JavaParameter> parameterList = javaMethod.getParameters();
        List<JavaParameterDTO> javaParameterDTOS = new ArrayList<>();
        //TODO 暂时未处理其他泛型,简单的使用Obj代替
        for (JavaParameter javaParameter : parameterList) {
            JavaParameterDTO javaParameterDTO = new JavaParameterDTO();
            javaParameterDTO.setName(javaParameter.getName());
            String typeToStr = javaParameter.getType().getFullyQualifiedName();
            String type = InitConstant.MAPPING.getOrDefault(typeToStr, typeToStr);
            javaParameterDTO.setType(type);
            javaParameterDTO.setCustomType(false);

            //设置默认值
            javaParameterDTO.setValue(InitConstant.VALUE.getOrDefault(type, null));

            for (String name : SKIP_PACKAGE) {
                if(type.contains(name)){
                    log.info("本类型在配置的包下，配置的包："+name+",类型："+type);
                    javaParameterDTO.setValue("null");
                }
            }

            String keyName = UUIDUtil.getID();
            javaParameterDTO.setKeyName(keyName);
            if (type.equals(typeToStr)) {
                //自定义类型 暂时处理一层包装类
                JavaClass javaClass = builder.getClassByName(typeToStr);
                log.info("自定义类型：" + typeToStr + "，cls：" + (javaClass==null?"null":javaClass.getFullyQualifiedName()) );
                if (javaClass != null) {
                    //TODO 框架无法识别第三方Jar包中的类
                    if(javaClass.isInterface()){

                        //获取实现类
                        List<JavaClass> javaClassList = javaClass.getDerivedClasses();
                        log.info("javaClassList1:" + javaClassList + ",自定义类型" + typeToStr );
                        if(!javaClassList.isEmpty()){
                            //取第一个
                            JavaClass javaClass1 = javaClassList.get(0);
                            javaParameterDTO.setCustomType(true);
                            List<JavaParameterDTO> javaParameterDTOList = new ArrayList<>();
                            //获取属性
                            addParameterToList(javaParameterDTOList, javaClass);
                            javaParameterDTOMap.put(keyName, javaParameterDTOList);
                        }else {
                            //接口的实现类没有，设置为null
                            javaParameterDTO.setValue("null");
                        }

                    }else if(javaClass.isEnum()){
                        //枚举取值
                        List<JavaField> javaFieldList = javaClass.getFields();
                        log.info("获取的枚举值："+javaFieldList+"，javaClass="+javaClass);
                        if(!javaFieldList.isEmpty()){
                            JavaField javaField = javaFieldList.get(0);
                            javaParameterDTO.setValue(javaField.getType().getFullyQualifiedName()+"."+javaField.getName());
                        }else {
                            javaParameterDTO.setValue("null");
                        }

                    }else {
                        javaParameterDTO.setCustomType(true);
                        List<JavaParameterDTO> javaParameterDTOList = new ArrayList<>();
                        //获取属性
                        addParameterToList(javaParameterDTOList, javaClass);

                        //获取父类属性 - 暂时也只获取一层
                        JavaClass superJavaClass = javaClass.getSuperJavaClass();

                        if ( !InitConstant.MAPPING.containsKey(superJavaClass.getFullyQualifiedName()) ) {
                            addParameterToList(javaParameterDTOList, superJavaClass);
                        }
                        log.info("superJavaClass:" + superJavaClass.getFullyQualifiedName() + ",自定义类型" + typeToStr + "，自定义类型中的类型：" + javaParameterDTOList);
                        javaParameterDTOMap.put(keyName, javaParameterDTOList);
                    }

                }else{
                    //说明不是项目中的类 - 设置为null
                    javaParameterDTO.setValue("null");
                }

            }

            javaParameterDTOS.add(javaParameterDTO);
        }
        return javaParameterDTOS;
    }

    /**
     * 获取属性，设置到对象中去
     *
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
            String fieldTypeStr = javaField.getType().getFullyQualifiedName();
            String fieldType = InitConstant.MAPPING.getOrDefault(fieldTypeStr, fieldTypeStr);
            JavaParameterDTO javaParameterDTO1 = new JavaParameterDTO();
            javaParameterDTO1.setKeyName("");
            javaParameterDTO1.setCustomType(false);
            javaParameterDTO1.setName(fieldName);
            javaParameterDTO1.setType(fieldType);
            javaParameterDTO1.setValue(InitConstant.VALUE.getOrDefault(fieldType, null));
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
            javaImplementsDTO.setType(javaType.getFullyQualifiedName());
            javaImplementsDTOList.add(javaImplementsDTO);
        }
        return javaImplementsDTOList;
    }

}