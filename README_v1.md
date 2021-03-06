# ElegantBus
[![](https://jitpack.io/v/codyer/component.svg)](https://jitpack.io/#codyer/component)

基于LiveData，实现eventBus，事件统一管理，动态APT生成，生命周期管理

## 说明
**其实使用LiveData实现基本的EventBus并不难，凡是看过基本原理的人，都可以轻松实现一套自己的EventBus，
唯一困难的地方就是，遇到粘性事件时会有一个bug，注册之前产生的事件有时候也会发射到后注册的接收者。
目前网上解决方案分两种：**
+ 方案1：用反射获取系统变量并干预
+ 方案2：通过修改包名取巧使用系统包可见变量

**因为无论是反射还是包可见变量访问，后期都会有可能出现兼容问题，甚至混淆的时候也有可能引入问题。
本方案使用包装类，对事件进行了统一的封装，通过框架自己记录注册版本来解决粘性事件问题，这种方案既不会有兼容问题，也不需要在混淆的时候进行特殊设置。
因此在此分享出来，希望可以给大家一个参考**

## 特点
- [x]  **支持分组处理，可设定激活状态**
- [x]  **自动生成事件相关代码，防止事件定义冲突**
- [x]  **支持自定义消息**
- [x]  整个生命周期（从onCreate到onDestroy）都可以实时收到消息
- [x]  激活状态（Started）可以实时收到消息，非激活状态（Stopped）无法实时收到消息，需等到Activity重新变成激活状态，方可收到消息
- [x]  支持AndroidX
- [x] 支持设置LifecycleObserver（如Activity）接收消息的模式：


## 关于分支

    master
    核心代码实现 + demo

    demo
    通过依赖方式引用master生成的库

    combine
    通过本地方式依赖master代码


## 项目依赖
### 目前只提供 AndroidX工程:（如果项目是新项目，建议升级）

```java
implementation 'com.github.codyer.ElegantBus:core:1.0.0'
```

### 使用注解生成事件管理接口
```java
annotationProcessor 'com.github.codyer.ElegantBus:compiler:1.0.0'
```

- [x]  升级方式--Refactor->Migrate to AndroidX
- [x]  项目降级--请自行降级 Refactor->Migrate App to AppCompat


### 使用介绍
#### 事件定义
- [x]  支持配置以区域为单位的事件开关
- [x]  统一事件管理
- [x]  根据实际情况开启关闭事件定义
- [x]  事件分领域（group）,不同领域同名事件互不干扰

```java
@EventGroup(name = "demo",active = true)
public enum AppDemo {
    @Event(description = "定义一个测试事件",data = String.class)testString,
    @Event(description = "定义一个测试事件测试对象",data = TestBean.class)testBean,
}
```


#### 订阅消息

- **observe**
### 【1】生命周期感知，不需要手动取消订阅，以下方式只会收到注册后发生的事件

```java
 ElegantBus.begin()
        .inGroup(Group$demo.class)// Group$***为自动生成的事件接口
        .withEvent$testBean()
        .observe(this, new ObserverWrapper<TestBean>() {
            @Override
            public void onChanged(TestBean testBean) {
            }
        });
```

- **observeAny**
### 【2】以下方式注册事件可以接收到注册前发生的事件（最后一次事件）

```java
ElegantBus.begin()
            .inGroup(Group$demo.class)
            .withEvent$testBean()
            .observeAny(this, new ObserverWrapper<TestBean>() {
                @Override
                public void onChanged(@Nullable TestBean testBean) {
                }
            });
 ```

- **observeForever**
### 【3】需要手动取消订阅

 ```java
ElegantBus.begin()
            .inGroup(Group$demo.class)
            .withEvent$testBean()
            .observeForever(new ObserverWrapper<TestBean>() {
                @Override
                public void onChanged(@Nullable TestBean testBean) {
                }
            });
```

#### 取消订阅

```java
ElegantBus.begin()
            .inGroup(Group$demo.class)
            .withEvent$testBean().
	        .removeObserver(observer);
```


### 【4】取消生命周期相关的所有监听

``` java
 ElegantBus.begin()
                .inGroup(Group$demo.class)
                .withEvent$testBean()
                .removeObservers(this);
```


#### 发送消息
- **setValue（Object o）**
### 在主线程发送消息

```java
ElegantBus.begin()
            .inGroup(Group$demo.class)
            .withEvent$testBean()
            .setValue(value);
```

- **postValue(Object o)**
### 在后台线程发送消息，订阅者会在主线程收到消息

```java
ElegantBus.begin()
            .inGroup(Group$demo.class)
            .withEvent$testBean()
            .postValue(value);
```


## 混淆规则
暂无

## core代码配置

``` java
compileSdkVersion 28

defaultConfig {
    minSdkVersion 19
    targetSdkVersion 28
    versionCode 1
    versionName 1.0.0
}
```

## 版本

版本 | 功能
---|---
1.0.0 | 初版，支持基本功能

## 为了方便统一管理，新的变更和优化移至component下面 
请参考[Component](https://github.com/codyer/component) 下的代码


## 主要功能提交记录
1. 主要功能完成（Apr. 3, 2019）


## 其他
- 欢迎提Issue与作者交流
- 欢迎提Pull request，帮助 fix bug，增加新的feature，让ElegantBus变得更强大、更好用


## 感谢如下作者提供新的思路，因为不想用反射，所以基于原作者做了重构，具体实现原理以及原文链接参见如下


#### 实现原理
- ElegantBus的实现原理可参见作者在美团技术博客上的博文：
[Android消息总线的演进之路：用LiveDataBus替代RxBus、EventBus](https://tech.meituan.com/Android_LiveDataBus.html)
- 该博文是初版ElegantBus的实现原理，与当前版本的实现可能不一致，仅供参考
1. [invoking-message](https://github.com/JeremyLiao/invoking-message) 消息总线框架，基于ElegantBus实现。它颠覆了传统消息总线定义和使用的方式，通过链式的方法调用发送和接收消息，使用更简单
