/*
 * souche.com
 * Copyright (C) 2013-2019 All Rights Reserved.
 */
package com.uifuture.maven.plugins;

import com.uifuture.maven.plugins.base.AbstractPlugin;
import com.uifuture.maven.plugins.dto.JavaClassDTO;
import com.uifuture.maven.plugins.util.JavaProjectBuilderUtil;
import com.uifuture.maven.plugins.util.PackageUtil;
import com.uifuture.maven.plugins.util.StringUtil;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 构建自动化测试
 * @author chenhx
 * @version UnittestPlugin.java, v 0.1 2019-05-30 17:47 chenhx
 */
@Mojo(name="test")
public class UnittestPlugin extends AbstractPlugin {
    /**
     * 需要测试的项目包名
     */
    @Parameter
    private String testPackageName;
    /**
     * 作者
     */
    @Parameter(defaultValue = "")
    private String author;
    /**
     * 是否获取子包下的类
     */
    @Parameter(defaultValue = "true")
    private Boolean childPackage;

    /**
     * 需要mock掉的包，包下所有类的方法都会被mock
     */
    @Parameter
    private String mockPackage;

    /**
     * @date
     */
    private static final String DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info( "开始构建自动化测试代码" +
                "\ntestPackageName:"+ testPackageName
                +"\nchildPackage："+childPackage
                +"\ntarget："+target
                +"\nbasedir："+basedir
                +"\nproject："+project
        );

        //获取该包下所有的类
        List<String> javaList = PackageUtil.getClassName(basedir.getPath(), testPackageName, childPackage);
        getLog().info( "获取的所有类名为："+javaList);
        //class文件绝对路径
        for (String javaName : javaList) {
            getLog().info("当前文件路径："+javaName);
            if(javaName.endsWith("$1")){
                //跳过
                getLog().info("跳过:"+javaName);
                continue;
            }
            //文件路径
            genTest(javaName);
            //先测试一个类
            break;
        }
    }


    private void genTest(String javaName) {
        try {
            //类名
            String className = javaName.substring(javaName.lastIndexOf("/")+1,javaName.lastIndexOf("."));

            Configuration cfg = getConfiguration();

            String testJavaName =basedir + "/src/test/java/" + testPackageName.replace(".", "/")+ "/"+className +"Test.java";
            getLog().info("生成的文件路径："+testJavaName+", className:"+className);
            File file = new File(testJavaName);
            if (file.exists()) {
                getLog().info(file+"已经存在，不进行生成");
                return;
            }
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                getLog().error(file.getParentFile()+"生成失败" );
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("date", DATE);
            data.put("author", author);
            data.put("modelNameUpperCamel", className);
            data.put("modelNameLowerCamel", StringUtil.strConvertLowerCamel(className));

            String mainJava = basedir + "/src/main/java/";
            //获取类中方法
            JavaClassDTO javaClassDTO = JavaProjectBuilderUtil.buildTestMethod(javaName,testPackageName+"."+className ,mainJava);
            data.put("javaClassDTO", javaClassDTO);

            //获取mock的类

            cfg.getTemplate("test.ftl").process(data, new FileWriter(file));

            getLog().info(file+"生成成功");
        } catch (Exception e) {
            getLog().error("生成失败，出现异常",e);
        }

    }


    private freemarker.template.Configuration getConfiguration() throws IOException {
        freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_28);
        if(configPackage.startsWith("/")){
            configPackage = configPackage.substring(1);
        }
        if(configPackage.endsWith("/")){
            configPackage = configPackage.substring(0,configPackage.length()-1);
        }
        File file = new File(basedir + "/" + configPackage);
        if(!file.exists()){
            getLog().error(file+"配置文件不存在");
            throw new RuntimeException("配置文件不存在");
        }
        cfg.setDirectoryForTemplateLoading(file);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
        return cfg;
    }



}