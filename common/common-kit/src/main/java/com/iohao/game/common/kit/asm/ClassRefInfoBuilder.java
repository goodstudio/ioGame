/*
 * ioGame
 * Copyright (C) 2021 - present  渔民小镇 （262610965@qq.com、luoyizhu@gmail.com） . All Rights Reserved.
 * # iohao.com . 渔民小镇
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.iohao.game.common.kit.asm;

import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import com.iohao.game.common.kit.StrKit;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 渔民小镇
 * @date 2022-01-02
 */
@Setter
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
class ClassRefInfoBuilder {
    Class<?> clazz;
    /** 构造 访问器 */
    ConstructorAccess<?> constructorAccess;
    /** 方法 访问器 */
    MethodAccess methodAccess;

    ClassRefInfo build() {
        ClassRefInfo classRefInfo = new ClassRefInfo();
        classRefInfo.constructorAccess = this.constructorAccess;
        classRefInfo.methodAccess = this.methodAccess;
        classRefInfo.clazz = this.clazz;

        // method - 以方法名作为 key
        classRefInfo.methodRefInfoMap = createMethodMap();

        // field - 以属性名作为 key
        classRefInfo.filedRefInfoMap = createFieldMap();

        return classRefInfo;
    }

    private Map<String, FieldRefInfo> createFieldMap() {
        List<Field> fieldList = listField();
        // list 转 map, 属性名作为key
        return fieldList.stream().collect(Collectors.toMap(Field::getName, field -> {
            // 开放访问权限
            field.setAccessible(true);

            // 属性名
            String fieldName = field.getName();

            // 首字母转换为大写
            fieldName = StrKit.firstCharToUpperCase(fieldName);

            // method 对应的 getter setter
            String methodGetName = "get" + fieldName;
            String methodSetName = "set" + fieldName;

            FieldRefInfo filedRefInfo = new FieldRefInfo();
            filedRefInfo.fieldName = fieldName;

            // 方法访问下标
            filedRefInfo.methodGetIndex = this.methodAccess.getIndex(methodGetName);
            filedRefInfo.methodSetIndex = this.methodAccess.getIndex(methodSetName);

            // 方法名
            filedRefInfo.methodGetName = methodGetName;
            filedRefInfo.methodSetName = methodSetName;

            // 返回值
            filedRefInfo.returnType = field.getType();

            // 原生 field
            filedRefInfo.field = field;

            return filedRefInfo;
        }));
    }

    private Map<String, MethodRefInfo> createMethodMap() {
        List<Method> methodList = listMethod();
        // list 转 map, 方法名作为 key
        return methodList.stream().collect(Collectors.toMap(Method::getName, method -> {
            // 方法名, 方法下标
            String methodName = method.getName();
            int methodIndex = this.methodAccess.getIndex(methodName);

            MethodRefInfo methodRefInfo = new MethodRefInfo();
            methodRefInfo.methodAccess = this.methodAccess;
            methodRefInfo.method = method;
            methodRefInfo.methodIndex = methodIndex;
            methodRefInfo.methodName = methodName;
            return methodRefInfo;
        }));

    }

    /**
     * 获取类的字段列表
     * <pre>
     *     包含父类的字段
     * </pre>
     *
     * @return 字段列表
     */
    private List<Field> listField() {
        // 类信息
        Class<?> nextClass = this.clazz;
        // bean 字段列表
        List<Field> fieldList = new ArrayList<>();

        // 获取类的字段, 并包含父类的
        while (nextClass != Object.class) {
            Field[] declaredFields = nextClass.getDeclaredFields();
            for (Field field : declaredFields) {
                int modifiers = field.getModifiers();
                // 静态的字段不需要
                if (Modifier.isStatic(modifiers)) {
                    continue;
                }
                fieldList.add(field);
            }
            // 父类 class
            nextClass = nextClass.getSuperclass();
        }

        return fieldList;
    }

    /**
     * 获取类的方法列表
     * <pre>
     *     包含父类的方法
     * </pre>
     *
     * @return 方法列表
     */
    private List<Method> listMethod() {
        // 类信息
        Class<?> nextClass = this.clazz;
        // bean 方法列表
        List<Method> methodList = new ArrayList<>();

        // 获取类的字段, 并包含父类的
        while (nextClass != Object.class) {
            Method[] declaredMethods = nextClass.getDeclaredMethods();
            for (Method method : declaredMethods) {
                int modifiers = method.getModifiers();
                // 静态的字段不需要
                if (Modifier.isStatic(modifiers) || method.isBridge()) {
                    continue;
                }
                methodList.add(method);
            }
            // 父类 class
            nextClass = nextClass.getSuperclass();
        }

        return methodList;
    }
}
