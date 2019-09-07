$(function () {

    history.pushState(null, null, document.URL);
    window.addEventListener('popstate', function () {
        history.pushState(null, null, document.URL);
    });

    /*1. 表单校验*/
    /*2. 提交登录请求*/
    /*3. 成功 跳转首页*/
    /*4. 不成功 根据错误信息给对应的表单加上错误提示*/
    /*5. 重置 表单内容的清空  检验样式的清除*/


    /*使用表单校验插件 bootstrapvalidator 插件*/
    /*1、下载  https://github.com/nghuuphuoc/bootstrapvalidator/tree/v0.5.3*/
    /*2. 文档  https://www.cnblogs.com/v-weiwang/p/4834672.html*/
    /*3. 文档  https://blog.csdn.net/u013938465/article/details/53507109*/

    /*开始使用*/
    /*1. 引入依赖资源  css  js */
    /*2. HTML遵循一定的规则  form-group > form-control */
    /*3. 初始化校验插件 扩展一个新的api*/
    var loginform = $('#loginForm');
    loginform.bootstrapValidator({
        submitButtons: '.bg_submit_btn',
        /*配置项*/
        /*配置表单元素的四种状态的图标 */
        /*未校验  无图标*/
        /*校验失败 错误图标*/
        /*校验成功 正确图标*/
        /*校验中   加载图标*/
        feedbackIcons: {
            valid: 'glyphicon glyphicon-ok',
            invalid: 'glyphicon glyphicon-remove',
            validating: 'glyphicon glyphicon-refresh'
        },
        /*配置校验规则*/
        /*表单内的所有需要校验字段*/
        fields: {
            /*需要分别去配置不同的校验规则*/
            /*通过表单元素的name属性去匹配*/
            username: {
                /*具体的规则  不止一个*/
                validators: {
                    /*指定校验规则*/
                    /*非空校验*/
                    notEmpty: {
                        message: '用户名必填'
                    },
                    /*定义一个自定义规则 必须加callback*/
                    callback: {
                        message: '用户名不存在'
                    }
                }
            },
            password: {
                validators: {
                    notEmpty: {
                        message: '密码必填'
                    },
                    /*更多校验规则*/
                    stringLength: {
                        min: 1,
                        max: 1,
                        message: '密码必须1个字符'
                    },
                    callback: {
                        message: '用户名或者密码错误'
                    }
                }
            }
        }
        //这是插件的自定义事件  校验成功的时候触发
    }).on('success.form.bv', function (e) {
        e.preventDefault();
        //提交登录请求
        $.ajax({
            url: e.currentTarget.action,
            type: 'post',
            data: loginform.serialize(),
            dataType: 'json',
            success: function (data) {
              if(data.error_code == 0){
                  location.href='./manager/main'
              }else{
                  loginform.data('bootstrapValidator').updateStatus('password', 'INVALID', "callback");
              }
            }
        });
    });
    // 回车登录事件
    $("body").keydown(function(event) {
        if (event.keyCode == "13") {//keyCode=13是回车键
            loginform.click();
        }
    });
});
