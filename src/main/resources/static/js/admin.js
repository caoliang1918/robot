$(function () {
    /*1.侧边栏显示隐藏*/
    $('[data-menu]').on('click',function () {
        $('aside').toggle();
        $('section').toggleClass('menu');
    });
    /*2.菜单的滑入滑出*/
    $('.menu a[href="javascript:;"]').on('click',function () {
        $(this).next('div').slideToggle();
    });
    /*3.退出功能*/
    var html = '<div class="modal fade" id="myModal">\n' +
                '    <div class="modal-dialog modal-sm">\n' +
                '        <div class="modal-content">\n' +
                '            <div class="modal-header">\n' +
                '                <button type="button" class="close" data-dismiss="modal"><span>&times;</span></button>\n' +
                '                <h4 class="modal-title">温馨提示</h4>\n' +
                '            </div>\n' +
                '            <div class="modal-body">\n' +
                '                <p class="text-danger"><span class="glyphicon glyphicon-exclamation-sign"></span> 您确定要退出后台管理系统吗？</p>\n' +
                '            </div>\n' +
                '            <div class="modal-footer">\n' +
                '                <button type="button" class="btn btn-default" data-dismiss="modal">取消</button>\n' +
                '                <button type="button" class="btn btn-primary">退出</button>\n' +
                '            </div>\n' +
                '        </div>\n' +
                '    </div>\n' +
                '</div>';
    $('body').append(html).on('click','#myModal .btn-primary',function () {
        /*退出*/
        $.ajax({
            url:'/employee/employeeLogout',
            type:'get',
            data:{},
            dataType:'json',
            success:function (data) {
                if(data.success){
                    location.href = '/admin/login.html';
                }
            }
        });
    });

    NProgress.configure({ showSpinner: false });
    /*ajax请求的进度显示*/
    /*1. 当发起ajax请求前 开始进度*/
    $(window).ajaxStart(function () {
        NProgress.start();
    });
    /*2. 当ajax响应结束  结束进度*/
    $(window).ajaxStop(function () {
        NProgress.done();
    });
});