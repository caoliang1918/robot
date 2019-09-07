window._app = {
    content: $("body .side-content:first")
    , getContentUrl: function(url){
        if(this._contentUrl){
            let _url = this._contentUrl + url;
            return _url.replace(/\/\//g,"\/");
        }
    }
    , scrollBar:[]
    , ajaxDomParam:{}
    , pageObj:[]
    , popoverList: []
    , initDatepicker: function (countCondition) {
        if(countCondition != undefined && countCondition.length > 0){
            let _key;
            let _ref;
            let _tmp;
            for(let i = 0; i < countCondition.length > 0; i++){
                if((_tmp = countCondition[i]) != null && (_key = _tmp.key) != undefined && (_key == 'endTime' || _key == 'startTime')){
                    _ref = _tmp.refEle;
                    if(_ref != undefined){
                        _ref.datepicker('setEndDate', new Date());
                        _ref.datepicker('setStartDate', null)
                    }
                }
            }
        }
    }
    , removePopoverList: function(){
        if(this.popoverList && this.popoverList.length > 0){
            for(let i = 0; i < this.popoverList.length; i++){
                this.popoverList[i].popover('hide');
            }
            this.popoverList = [];
        }
    }
    , doPopoverList: function(dom){
        if(dom){
            let tdPopovers = dom.find("td.td-popover");
            if(tdPopovers){
                this.popoverList.push(tdPopovers);
                tdPopovers.css("cursor","pointer");
                tdPopovers.popover('toggle');
            }
        }

    }
    , pageReLoad: function (key) {
        let _page
        for(let i = 0; i < this.pageObj.length; i++){
            _page = this.pageObj[i]
            if(_page.id == key){
                _page.reload();
            }
        }
    }
    , pageInitLoad: function (key) {
        let _page
        for(let i = 0; i < this.pageObj.length; i++){
            _page = this.pageObj[i]
            if(_page.id == key){
                _page.ajaxParams.param.pageNumber = 1;
                _page.ajaxParams.param._fi = -666;
                _page.reload();
                return ;
            }
        }
    }
    , addPageParam: function(key, obj){
        if(!key || !obj){
            return ;
        }
        let _page
        for(let i = 0; i < this.pageObj.length; i++){
            _page = this.pageObj[i]
            if(_page.id == key){
                let keys = Object.keys(obj);
                for(let j = 0; j < keys.length; j ++){
                    _page.ajaxParams.param[keys[j]] = obj[keys[j]];
                }
                // console.log(_page.ajaxParams.param);
                return ;
            }
        }
    }
    , setScrollBarParam: function (paramObj) {
        if(paramObj.id != undefined && this.scrollBar.length > 0){
            let scrollBarEntity;
            for(let i=0; i < this.scrollBar.length; i++){
                scrollBarEntity = this.scrollBar[i];
                if(scrollBarEntity.id == paramObj.id){
                    delete paramObj.id
                    let key;
                    let keys = Object.keys(paramObj);
                    for(let j=0; j < keys.length; j++){
                        key = keys[j];
                        scrollBarEntity.param[key] = paramObj[key];
                    }
                    scrollBarEntity.param.pageNumber = 1;
                    scrollBarEntity._tbody.empty();
                    _app.ajax(scrollBarEntity);
                }
            }

        }

    }
    , removeScrollBar: function () {
        if(this.scrollBar && this.scrollBar.length > 0){
            for(let i=0; i < this.scrollBar.length; i++){
                $(window).off("scroll", this.scrollBar[i].scrollBarListion);
            }
            _app.scrollBar = [];
        }

    }
    , jsvoid: "javascript:void(0);"
    , ajax: function (obj) {
        if (obj && obj.url) {
            if (obj.param == undefined) {
                obj.param = {};
            }
            if(obj._tmpName != undefined && obj.func == undefined){
                let _loadFunc;
                if(obj.id != undefined && obj.id.length > 0){
                    _loadFunc = this.ajaxDomParam[obj.id];
                    if(_loadFunc){
                        delete this.ajaxDomParam[obj.id];
                    }
                }
                obj.func = function (data) {
                    if(!data){
                        return ;
                    }
                    if(!data.result){
                        data.result = {};
                    }
                    let _Dom;
                    if(obj._load == true && obj._html){
                        _Dom = obj._html;
                    }else if(obj._loadEle && obj._loadEle.length > 0){
                        _Dom = obj._loadEle;
                    }else{
                        _Dom = _app.content.find(".wrapper-content:first");
                    }
                    _app.removePopoverList();
                    _Dom.html(template(obj._tmpName, {data: data.result}));
                    let _iChecks = _Dom.find('.i-checks');
                    if(_iChecks && _iChecks.length > 0){
                        _iChecks.iCheck({
                            checkboxClass: 'icheckbox_square-green',
                            radioClass: 'iradio_square-green',
                        });
                    }
                    if(_loadFunc){
                        _loadFunc(data.result, _Dom);
                    }
                    _app.ajaxContentEle(_Dom);
                    _app.ajaxHrefEle(_Dom);

                }
            }else if(obj._load == true && obj._html != undefined){
                obj.dataType = 'html';
                obj.func = function (data) {
                    _app.removePopoverList();
                    this._html.html(data)
                }
            }
            if(obj.async === undefined){
                obj.async = true;
            }
            $.ajax({
                url: obj.url
                , headers: {Acceptable: 'asyn-content'}
                , data: obj.param
                , async: obj.async
                , type : 'POST'
                , dataType: obj.dataType != undefined ? obj.dataType : obj.func != undefined || obj.redirect != undefined ? 'json' : 'html'
                , success: function (data) {
                    if(data == "{\"code\":-5927}"){
                        window.location.href = _app.getContentUrl("/welcome");
                        return ;
                    }
                    if (obj.redirect){
                        if(data.code == 200000){
                            let robj = {url: obj.redirect};
                            _app.ajax(robj);
                        }
                    }else if(obj.func){
                        obj.func(data);
                    }else {
                        _app.removeScrollBar();
                        _app.removePopoverList();
                        _app.content.html(data);
                    }
                    if(obj.id){
                        delete _app.ajaxDomParam[obj.id];
                    }
                    _app.ajaxHrefEle(_app.content);
                    _app.ajaxContentEle(_app.content);
                }
                , error: function (data) {
                    console.log(data);
                    _app.removeScrollBar();
                    _app.ajaxDomParam = {};
                    window.location.href = _app.getContentUrl("/welcome");
                }
            })
        }
    }
    , ajaxHrefEle: function (ele) {
        if (ele) {
            let eles = ele.find("a.menu-entity[href]");
            if(eles){
                let _js = _app.jsvoid;
                eles.each(function () {
                    let obj = {};
                    obj.url = $(this).attr("href");
                    if (_js != obj.url) {
                        $(this).attr("href", _js);
                        let param = $(this).attr("data-params");
                        if(param){
                            $(this).removeAttr("data-params");
                            obj.param = _app.serializeParse(param);
                        }
                        let load = $(this).attr("data-load");
                        $(this).click(function () {
                            _app.removeScrollBar();
                            _app.ajax(obj);
                        })
                        if(load){
                            $(this).removeAttr("data-load");
                            $(this).click();
                        }
                    }
                });
            }
            return eles;
        }
    }
    , ajaxContentEle: function(content){
        if(content){
            let eles = content.find("[data-type='ajax'][data-url]");
            if(eles){
                eles.each(function () {
                    $(this).removeAttr("data-type");
                    let obj = {};
                    obj.url = $(this).attr("data-url");
                    $(this).removeAttr("data-url");
                    obj.param = $(this).attr("data-params");
                    if(obj.param){
                        $(this).removeAttr("data-params");
                        obj.param = _app.serializeParse(obj.param);
                    }
                    let _tmpjs = $(this).attr("data-tmpjs");
                    if(_tmpjs){
                        obj._tmpName = _tmpjs;
                        $(this).removeAttr("data-tmpjs");
                    }
                    let _code = $(this).attr("data-code");
                    if(_code){
                        $(this).removeAttr("data-code");
                        obj.id = _code;
                    }
                    let _loadEle = $(this).attr("data-loadEle");
                    if(_loadEle){
                        $(this).removeAttr("data-loadEle");
                        let _loadDom = $(_loadEle);
                        if(_loadDom.length > 0){
                            obj._loadEle = _loadDom;
                        }
                    }
                    let _option = $(this).attr("data-option");
                    if(_option){
                        $(this).removeAttr("data-option");
                        switch (_option) {
                            case 'form':
                                _option = $(this).parents("form:first")
                                if(_option){
                                    obj.param = _option.serialize()
                                }
                                break;
                            case 'page':
                                obj.type = _option;
                                obj.async = false;
                                let _pageCfg = _app.ajaxDomParam[_code];
                                if(_pageCfg && obj.id){
                                    _pageCfg.dom = $(this);
                                    _pageCfg.url = obj.url;
                                    _pageCfg.id = obj.id;
                                    let _pageEle =  _app.page(_pageCfg);
                                    _app.pageObj = [];
                                    _app.pageObj.push(_pageEle);
                                    _pageEle.doPage();
                                    delete _app.ajaxDomParam[_code];
                                }
                                // _app.windowsScroll(obj);
                                return true
                        }
                    }
                    obj.redirect = $(this).attr("data-redirect");
                    let load = $(this).attr("data-load");
                    if(load){
                        $(this).removeAttr("data-load");
                        obj._load = true;
                        obj._html = $(this);
                        _app.ajax(obj);
                    }else{
                        $(this).click(function () {
                            _app.ajax(obj);
                        });
                    }
                });
            }
        }
    }
    , serialize: function (jsondata) {
        if (jsondata) {
            let keys = Object.keys(jsondata);
            if (keys) {
                let result = "";
                let key;
                for (let i = 0; i < keys.length; i++) {
                    key = keys[i];
                    result += (key + '=' + jsondata[key] + "&");
                }
                return result.substring(0, result.length - 1);
            }
        }
    }
    , serializeParse: function (strSerialize) {
        if (strSerialize) {
            let arr = strSerialize.split("&");
            if (arr) {
                let entity;
                let obj = {};
                let tmpArr;
                for (let j = 0; j < arr.length; j++) {
                    entity = arr[j];
                    tmpArr = entity.split("=")
                    obj[tmpArr[0]] = tmpArr[1];
                }
                return obj;
            }
        }
    }
    , windowsScroll: function(obj){
        if(obj && obj.url && obj.param){
            obj._viewHeight= 0
            obj._tbody = _app.content.find(".wrapper-content tbody:first");
            if(!obj._tbody){
                console.log("未查询到 page tbody!")
                return ;
            }
            obj._changeHeight = 0
            obj._doAjax = true
            obj._scrollBar = 0
            obj._init = function () {
                _app.ajax(obj)
                return this;
            }
            obj.func = function (data) {
                if(data.result && data.result.result && data.result.result.length > 0){
                    $(template(this._tmpName, {data: data.result.result})).appendTo(this._tbody);
                    if(this._viewHeight === 0){
                        this._viewHeight =  $(window).height();
                    }
                    this._changeHeight = this._tbody.height();
                    if(this.param.pageNumber){
                        this.param.pageNumber++;
                    }
                    _app.ajaxContentEle(_app.content);
                }
                this._doAjax = true;
            }
            obj._init();
            let scroll = function(event){
                obj._scrollBar = $(this).scrollTop();
                if(obj._changeHeight - obj._scrollBar < obj._viewHeight){
                    if (obj._doAjax) {
                        // console.log("do.....");
                        obj._doAjax = false
                        // console.log("-----"+_height)
                        // console.log("+++++++"+_length)
                        _app.ajax(obj)
                    }
                }
            };
            $(window).on("scroll", scroll);
            obj.scrollBarListion = scroll;
            _app.scrollBar.push(obj)
        }
    }
    , treeview: function(idStr, nodeArr, viewfunc){
        let setting = {
            view: {
                addHoverDom: function addHoverDom(treeId, treeNode) {
                    let sObj = $("#" + treeNode.tId + "_span");
                    if (treeNode.editNameFlag || $("#addBtn_"+treeNode.tId).length>0) return;
                    let addStr = "<span class='button add' id='addBtn_" + treeNode.tId+ "' title='add node' onfocus='this.blur();'></span>";
                    sObj.after(addStr);
                    let btn = $("#addBtn_"+treeNode.tId);
                    let new_count = this.newCount;
                    if (btn) btn.bind("click", function(){
                        let zTree = $.fn.zTree.getZTreeObj(idStr);
                        zTree.addNodes(treeNode, {id:(100 + new_count), pId:treeNode.id, name:"new node" + (new_count++)});
                        return false;
                    });
                },
                removeHoverDom: function removeHoverDom(treeId, treeNode) {
                    $("#addBtn_"+treeNode.tId).unbind().remove();
                },
                viewHoverDom: function viewHoverDom(treeId, treeNode){
                    let sObj = $("#" + treeNode.tId + "_span");
                    let _id = treeNode.tId+ "_view";
                    if (treeNode.editNameFlag || $("#"+ _id).length>0) return;
                    let addStr = "<span class='button view' id='" + _id+ "' title='view' onfocus='this.blur();'></span>";
                    sObj.after(addStr);
                    let btn = $("#"+_id);
                    if (btn) btn.bind("click", function(){
                        if(treeNode.id){
                            if(viewfunc){
                                viewfunc(treeNode);
                            }else{
                                _app.ajax({url:_app.getContentUrl("/main/departmentUser/index"), param: {companyId: treeNode.companyId, departmentId: treeNode.id}});
                            }
                        }
                        return false;
                    });
                },
                selectedMulti: false
            },
            check: {
                enable: true
            },
            data: {
                simpleData: {
                    enable: true
                }
            },
            edit: {
                enable: true
            },
            newCount: 1
        };
        return $.fn.zTree.init($("#"+idStr), setting, nodeArr);
    }
    , treeData: function(treeEle){
        if(!treeEle._data){
            treeEle._data = function(nodes){
                if(nodes && nodes.length > 0){
                    let arr = [];
                    let subEntity;
                    let saveEntity;
                    let childrensEntity = [];
                    for(let index = 0; index < nodes.length; index ++){
                        subEntity = nodes[index];
                        saveEntity = {id: subEntity.id, pId: subEntity.pId, name: subEntity.name};
                        if(subEntity.children && subEntity.children.length > 0){
                            childrensEntity = treeEle._data(subEntity.children)
                            if(childrensEntity && childrensEntity.length > 0){
                                saveEntity.children = childrensEntity;
                            }
                        }
                        arr.push(saveEntity);
                    }
                    return arr;
                }
            }
        }
        return treeEle._data(treeEle.getNodes());
    }
    , doBootstrapValidator: function(obj){
        if(obj && obj.formEle){
            if(obj.submitEle == undefined){
                obj.submitEle = ".bg_submit_btn";
            }
            let $submitEle = $(obj.submitEle);
            if(!$submitEle || $submitEle.length <= 0){
                return;
            }
            let $ele = $(obj.formEle);
            if(!$ele || $ele.length <= 0){
                return;
            }
            obj.submitEle = obj.formEle + " " + obj.submitEle;
            obj.formEle = $ele;
            if(!obj.fields || obj.fields.length <= 0){
                return;
            }
            if(obj.prevload){
                obj.prevload();
            }
            if(obj.func){
                obj.formEle.bootstrapValidator({
                    submitButtons: obj.submitEle,
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
                    fields: obj.fields
                    //这是插件的自定义事件  校验成功的时候触发
                }).on('success.form.bv', function (e) {
                    e.preventDefault();
                    //提交登录请求
                    $.ajax({
                        url: e.currentTarget.action,
                        type: 'post',
                        data: obj.formEle.serialize(),
                        dataType: 'json',
                        success: function (data) {
                            obj.func(data);
                        }
                    });
                });
                obj.formEle.keydown(function(event) {
                    if (event.keyCode == "13") {//keyCode=13是回车键
                        obj.formEle.click();
                    }
                });
            }
        }

    }
    ,dataUtils:{
        timeStampToStr: function (val) {
            return new Date(parseInt(nS) * 1000).toLocaleString().replace(/:\d{1,2}$/,' ');
        }
    }
    , page: function (obj) {
        let _error = function(mark){
            return {mark:mark
                , doPage: function(){
                    switch (this.mark) {
                        case 0: console.error("page init error, need obj config"); break;
                        case 1: console.error("page init error, need obj.property: table config"); break;
                        case 2: console.error("page init error, need obj.property: table.column config"); break;
                        case 3: console.error("page init error, need obj.property: dom config"); break;
                        case 4: console.error("page init error, need obj.property: template config"); break;
                        case 5: console.error("page init error, need obj.property: template.header config"); break;
                        case 6: console.error("page init error, need obj.property: template.body config"); break;
                        case 8: console.error("page init error, need obj.property: ajax.url config"); break;
                        case 9: console.error("page init error, need root DOM ELE!"); break
                        case 11: console.error("page init error, need page content, Please define ._page_content DOM ELE!"); break;
                        case 12: console.error("page init error, need page totalPage, Please define ._page_total and code DOM ELE!"); break;
                        case 13: console.error("page init error, need page total, Please define ._page_total and mark DOM ELE!"); break;
                        case 12: console.error("page init error, need page current, Please define ._page_total and kbd DOM ELE!"); break;
                    }
                }}
        }
        if(obj == undefined){
            return _error(0);
        }
        if(obj.table == undefined){
            return _error(1);
        }
        if(obj.table.column == undefined){
            return _error(2);
        }
        if(obj.dom == undefined){
            return _error(3);
        }
        if(obj.template == undefined){
            return _error(4);
        }
        if(obj.template.header == undefined){
            return _error(5);
        }
        if(obj.template.body == undefined){
            return _error(6);
        }
        if(obj.url == undefined){
            return _error(8);
        }

        let _obj = {
            // table:{column: [{name:'登录名', key: 'login_name'}, {name:'用户名', key: 'user_name'}, {name:'配置时间', key: 'date'}, {name:'登录时间', key: 'ltime'}]
            //     , keys: {checkbox: 'nihao[]'}
            //     , content: {}
            //     , totalPage: {}
            //     , total: {}
            //     , prev: {}
            // },
            // _dom: {},
            // template: {header: 'page-header', body: 'page-tbody-sm',bodyOption: 'page-tbody-option-sm'},
            // ajax:{param:{}, func: function (dom) {}},
            // countCondition: [{name:'login_name', type: 'input'}, {name:'_del', type:'i', func: function(){}}],
            // countCondition_js: {},
            // load: function(){};
            ajaxParams:{
                // url: _app.getContentUrl("/manager/admin/page"),
                // param: {pageNumber: 1},
                 totalPage: 0
                , total: 0
                , size: 0
                // , data:[{login_name:'你好',user_name: '嘻嘻', date: 1560000125487, ltime: 1563200125487}, {login_name:'你好1',user_name: '嘻嘻1', date: 1560100225487, ltime: 1560130325487}]
                , func: function(data){
                    _app.removePopoverList();
                    if(data && data.error_code == 0){
                        // this.total = 0;
                        // this.totalPage = 0;
                        this.size = 0;
                        this.data = [];
                        if(this.param._fi){
                            this.total = data.result.total;
                            this.totalPage = data.result.pageTotal;
                            this.size = data.result.pageSize;
                            delete this.param._fi;
                        }
                        if(data.result.result){
                            if(data.result.total && data.result.total > 0){
                                this.total = data.result.total;
                            }
                            if(data.result.pageTotal && data.result.pageTotal > 0){
                                this.totalPage = data.result.pageTotal;
                            }
                            if(data.result.pageSize && data.result.pageSize > 0){
                                this.size = data.result.pageSize;
                            }
                            this.data = data.result.result;
                        }
                        let docstmfunc = true;
                        if(this.successAfterFunc){
                            if(!this.successAfterFunc(this.data, this.cstmfunc, this.dom)){
                                docstmfunc =false;
                            }
                        }
                        if(docstmfunc && this.cstmfunc){
                            this.cstmfunc(this.dom);
                        }
                    }
                }
            }
            , control: {next: true, prev: true}
            , getPageId: function(_this, isNext){
                if(_this.ajaxParams.data){
                    let _id_tmp;
                    let _id_old = _this.ajaxParams.param._id;
                    _this.ajaxParams.param._id = -777;
                    for(var i = 0; i < _this.ajaxParams.data.length; i ++){
                        _id_tmp = _this.ajaxParams.data[i].id;
                        if(_id_tmp && _id_tmp > 0){
                            if(isNext){
                                if(_this.ajaxParams.param._id < 0 || _id_tmp < _this.ajaxParams.param._id){
                                    _this.ajaxParams.param._id = _id_tmp;
                                }
                            }else{
                                if(_this.ajaxParams.param._id < 0 || _id_tmp > _this.ajaxParams.param._id){
                                    _this.ajaxParams.param._id = _id_tmp;
                                }
                            }
                        }
                    }
                    if(_this.ajaxParams.param._id == -777){
                        _this.ajaxParams.param._id = _id_old;
                    }
                }
            }
            , doPage: function(){
                if(this._dom && this._dom.length > 0){
                    let _this = this;
                    this.ajaxParams.successAfterFunc = function(data, callback, dom){
                        _this.table.total.text(_this.ajaxParams.total);
                        _this.table.totalPage.text(_this.ajaxParams.totalPage);
                        _this.table.current.text(_this.ajaxParams.param.pageNumber);
                        if(_this.ajaxParams.param.pageNumber <= 1){
                            _this.doPrev('off');
                        }else{
                            _this.doPrev();
                        }
                        if(_this.ajaxParams.param.pageNumber >= _this.ajaxParams.totalPage){
                            _this.doNext('off');
                        }else{
                            _this.doNext();
                        }
                        _this._dom.html(template(_this.template.header, _this));
                        _this._dom.find('.i-checks').iCheck({
                            checkboxClass: 'icheckbox_square-green',
                            radioClass: 'iradio_square-green',
                        });
                        let _js_switch = document.querySelectorAll('.js-switch');
                        if(_js_switch){
                            _js_switch.forEach(function (html) {
                                let _tmpEle = new Switchery(html, { color: '#1AB394', size:"large"});
                                let _do = true;
                                html.onchange = function() {
                                    let _state = html.checked;
                                    if(_do){
                                        _app.ajax({url: _this.ajaxParams.js_switch_url, param: {id: html.name, state: _state}, async : false, dataType: 'json'
                                            , func: function (data) {
                                                if(data.result == 1){
                                                    return ;
                                                }else{
                                                    if(_state){
                                                        _tmpEle.setPosition(true);
                                                    }else{
                                                        _tmpEle.setPosition(false);
                                                    }
                                                    _do = false;
                                                    _tmpEle.handleOnchange(true);
                                                }
                                            }});
                                    }else{
                                        _do = true;
                                    }
                                };
                            })
                        }
                        // _js_switch.addEventListener('click', function(){
                        //     console.log(this);
                        // })
                        _app.ajaxContentEle(_this._dom);
                        if(callback){
                            callback(dom)
                            return true;
                        }
                    }

                    let doInitPage = function(){
                        let viewBtn = function(prev, next, isprev){
                            if(prev && isprev){
                                _this.ajaxParams.param._s=0;
                                if(_this.ajaxParams.param.pageNumber > 1){
                                    _this.ajaxParams.param.pageNumber--;
                                    _this.doPrev()
                                }else{
                                    _this.doPrev('off')
                                }
                                _this.getPageId(_this, false);
                            }
                            if(next && !isprev){
                                _this.ajaxParams.param._s=1;
                                let _total = _this.ajaxParams.totalPage;
                                if(_this.ajaxParams.param.pageNumber < _total){
                                    _this.ajaxParams.param.pageNumber++;
                                    _this.doNext();
                                }else{
                                    _this.doNext('off');
                                }
                                _this.getPageId(_this, true);
                            }

                        }

                        _this.table.prev = _this.table.content.find("button.btn-white i.fa-arrow-left").parent();
                        _this.table.next = _this.table.content.find("button.btn-white i.fa-arrow-right").parent();

                        if(_this.table.prev && _this.table.prev.length > 0 && _this.table.next && _this.table.next.length > 0){
                            _this.table.prev.click(function(){
                                if(_this.control.prev){
                                    viewBtn(_this.control.prev, _this.table.next, 'isprev')
                                    _app.ajax(_this.ajaxParams);
                                }
                            });
                            _this.table.next.click(function(){
                                if(_this.control.next){
                                    viewBtn(_this.control.prev, _this.table.next)
                                    _app.ajax(_this.ajaxParams);
                                }
                            });
                        }
                    }
                    doInitPage();

                    let doInitCountCondition = function(){
                        let doInitCountConditionEle = function (eleJson) {
                            if(eleJson){
                                let _name = eleJson.name;
                                if(_name){
                                    let _ele;
                                    let _type = eleJson.type;
                                    if(!_type){
                                        _type = 'input';
                                    }
                                    _ele = _this.table.content.find(_type+'[name="'+_name+'"]:first');
                                    if(_ele && _ele.length > 0){
                                        if(eleJson.onEvt){
                                            switch (eleJson.onEvt) {
                                                case 'select':
                                                case 'change':
                                                    _ele.change(function () {
                                                        if(eleJson.func){
                                                            eleJson.func(_this.table.content, _this.ajaxParams, _ele, _this.countCondition);
                                                        }else{
                                                            let _param = {};
                                                            let _isDel = false;
                                                            let _vals = $(this).val();
                                                            if(!_vals){
                                                                _vals = '';
                                                                _isDel = true;
                                                            }
                                                            _param[_name] = _vals;
                                                            _this.changeAjaxParams(_param, _isDel);
                                                        }
                                                    });
                                                    return;
                                                case 'click':
                                                    _ele.click(function () {
                                                        if(eleJson.func){
                                                            eleJson.func(_this.table.content, _this.ajaxParams, _ele, _this.countCondition);
                                                        }else{
                                                            let _param = {};
                                                            _param[_name] = $(this).val();
                                                            _this.changeAjaxParams(_param);
                                                        }
                                                    });
                                                    return;
                                                case 'datepicker':
                                                    if(eleJson.key){
                                                       let datepickerTmp =  _ele.datepicker({
                                                            language: "zh-CN",
                                                            keyboardNavigation: false,
                                                            forceParse: false,
                                                            format: "yyyy-mm-dd",
                                                            language: 'cn',
                                                            endDate:new Date(),
                                                            autoclose: true
                                                        }).on('changeDate', function (e) {
                                                            let paramsPage = {};
                                                            paramsPage[eleJson.key] = e.date ? e.date.getTime(): '';
                                                            _this.changeAjaxParams(paramsPage, e.date ? false : true);
                                                           if(eleJson.key == 'startTime'){
                                                               if(eleJson.refEle != undefined){
                                                                   if(e.date == undefined){
                                                                       eleJson.refEle.datepicker('setStartDate', null)
                                                                   }else{
                                                                       eleJson.refEle.datepicker('setStartDate', new Date(e.date.valueOf()))
                                                                   }
                                                               }
                                                           }else if(eleJson.key == 'endTime'){
                                                               if(eleJson.refEle != undefined){
                                                                   if(e.date == undefined){
                                                                       eleJson.refEle.datepicker('setEndDate', new Date());
                                                                   }else{
                                                                       eleJson.refEle.datepicker('setEndDate', new Date(e.date.valueOf()));
                                                                   }
                                                               }
                                                           }
                                                        });
                                                       switch(eleJson.key){
                                                           case 'startTime':
                                                           case 'endTime':
                                                               let _findKey = eleJson.key == 'startTime' ? 'endTime' : 'startTime';
                                                               let _tmpEle;
                                                               for(let i = 0; i < _this.countCondition.length; i++){
                                                                   _tmpEle = _this.countCondition[i];
                                                                   if(_tmpEle != null && _tmpEle.key == _findKey){
                                                                       _tmpEle['refEle'] = datepickerTmp;
                                                                       break;
                                                                   }
                                                               }
                                                       }
                                                       // if(!_this.countCondition_js){
                                                       //     _this.countCondition_js = {};
                                                       // }
                                                       //  _this.countCondition_js[eleJson.key] = datepickerTmp;
                                                    }
                                                    return;
                                            }
                                        }
                                        if(eleJson.func){
                                            _ele.click(function () {
                                                eleJson.func(_this.table.content, _this.ajaxParams, _ele, _this.countCondition);
                                            })
                                            return ;
                                        }
                                        switch (_type) {
                                            case 'input':
                                                _ele.change(function () {
                                                    let _param = {};
                                                    let _isDel = false;
                                                    let _vals = $(this).val();
                                                    if(!_vals){
                                                        _vals = '';
                                                        _isDel = true;
                                                    }
                                                    _param[_name] = _vals;
                                                    _this.changeAjaxParams(_param, _isDel);
                                                });
                                                break;
                                            default:

                                        }

                                    }
                                }
                            }
                        }
                        if(_this.countCondition){
                            let _entity;
                            for(let i = 0; i < _this.countCondition.length; i++){
                                _entity = _this.countCondition[i];
                                doInitCountConditionEle(_entity);
                            }
                        }
                    }
                    doInitCountCondition();
                    _app.ajax(this.ajaxParams);

                }
            }
            , doPrev: function (off) {
                if(off){
                    this.control.prev = false;
                    this.table.prev.attr("disabled",true);
                }else{
                    this.table.prev.attr("disabled",false);
                    this.control.prev = true;
                }
            }
            , doNext: function (off) {
                if(off){
                    this.control.next = false;
                    this.table.next.attr("disabled",true);
                }else{
                    this.table.next.attr("disabled",false);
                    this.control.next = true;
                }
            }
            , changeAjaxParams: function (obj, isDel) {
                if(obj){
                    if(!this.ajaxParams.param){
                        this.ajaxParams.param = {pageNumber: 1};
                    }else if(!this.ajaxParams.param.pageNumber){
                        this.ajaxParams.param.pageNumber = 1;
                    }
                    let _keys = Object.keys(obj);
                    let _key;
                    for(let i = 0; i < _keys.length; i++){
                        _key = _keys[i];
                        if(isDel){
                            delete this.ajaxParams.param[_key];
                            // console.log(this.ajaxParams.param);
                        }else{
                            this.ajaxParams.param[_key] = obj[_key];
                            // console.log(this.ajaxParams.param);
                        }
                    }
                }
            }
            , reload: function () {
                _app.ajax(this.ajaxParams);
            }
        }

        if(obj.id){
            _obj.id = obj.id;
        }
        _obj._dom = obj.dom;
        if(!_obj._dom || _obj._dom.length <= 0){
            return _error(9);
        }
        _obj.table = obj.table;
        _obj.table.content = _obj._dom.parents("._page_content:first");
        if(!_obj.table.content || _obj.table.content <= 0){
            return _error(11);
        }
        _obj.table.totalPage = _obj.table.content.find("._page_total code")
        if(!_obj.table.totalPage || _obj.table.totalPage <= 0){
            return _error(12);
        }
        _obj.table.total = _obj.table.content.find("._page_total mark")
        if(!_obj.table.total || _obj.table.total <= 0){
            return _error(13);
        }
        _obj.table.current = _obj.table.content.find("._page_total kbd")
        if(!_obj.table.current || _obj.table.current <= 0){
            return _error(14);
        }
        if(obj.prevload){
            _obj.prevload = obj.prevload;
            _obj.prevload();
        }
        _obj.template = obj.template;
        _obj.ajaxParams.url = obj.url;
        if(obj.ajax){
            if(obj.ajax.param){
                _obj.ajaxParams.param = obj.ajax.param;
            }
            if(obj.ajax.func){
                _obj.ajaxParams.cstmfunc = obj.ajax.func;
                _obj.ajaxParams.dom = _obj.table.content;
            }
        }
        _obj.ajaxParams.async = false;
        _obj.changeAjaxParams({_fi: -666});
        if(obj.countCondition){
            _obj.countCondition = obj.countCondition;
        }
        return _obj
    }
    , pageCfg: function (func) {
        if(func){
            setTimeout(func, 300);
        }
    }
    , sidebarList: function (obj) {
        if(obj != null && obj.length > 0){
            obj.click(function () {
                let audios = $(this).parents("div#right-sidebar:first").find("audio");
                if(audios != null && audios.length > 0){
                    let audio;
                    audios.each(function () {
                        audio = $(this)[0]
                        if(!audio.paused){
                            audio.pause();
                        }
                    })
                }
                $(this).parents("div#right-sidebar:first").removeClass('sidebar-open');
            })

        }
    }
}
var _dateStrToTime = function (StringDate) {
    return (new Date(Date.parse(StringDate.replace(/-/g,"/")))).getTime();
}
var _dateFormat = function(date, format){
    this.obj = new Object();
    this.obj.date = date;
    this.obj.format = function(){
        if(!this.date){
            return "";
        }
        this.date = new Date(this.date);
        this.y = this.date.getFullYear();
        this.m = this.date.getMonth() + 1;
        this.m = this.m < 10 ? ('0' + this.m) : this.m;
        this.d = this.date.getDate();
        this.d = this.d < 10 ? ('0' + this.d) : this.d;
        this.h = this.date.getHours();
        this.h = this.h < 10 ? ('0' + this.h) : this.h;
        this.minute = this.date.getMinutes();
        this.second = this.date.getSeconds();
        this.milliseconds = this.date.getMilliseconds()
        this.minute = this.minute < 10 ? ('0' + this.minute) : this.minute;
        this.second = this.second < 10 ? ('0' + this.second) : this.second;
        this.milliseconds = this.milliseconds < 10 ? ('00' + this.milliseconds) : this.milliseconds < 100 ? ("0" + this.milliseconds) : this.milliseconds;
        // return this.y + '-' + this.m + '-' + this.d+' '+this.h+':'+this.minute+':'+this.second+':'+this.milliseconds;
        if(!format){
            format = 0;
        }
        let tmp
        switch (format) {
            case 0: return this.y + '-' + this.m + '-' + this.d+' '+this.h+':'+this.minute+':'+this.second;
            case 1: return this.h+':'+this.minute;
            case 2:
                tmp = (this.h-8);
                tmp = tmp < 0 ? this.d > 0 ? (tmp + 23) : 0 : tmp;
                return (tmp  === 0 ? "00": tmp < 10 ? ("0"+tmp): tmp)+':'+this.minute+ ':' + this.second+':'+this.milliseconds;
            case 3:
                tmp = (this.h-8);
                tmp = tmp < 0 ? this.d > 0 ? (tmp + 23) : 0 : tmp;
                return (tmp  === 0 ? "00": tmp < 10 ? ("0"+tmp): tmp)+':'+this.minute+ ':' + this.second;
            case 4:
                return this.minute+ ':' + this.second+':'+this.milliseconds;
            case 5:
                return this.y + '-' + this.m + '-' + this.d;
            default: return this.y + '-' + this.m + '-' + this.d+' '+this.h+':'+this.minute+':'+this.second+':'+this.milliseconds;

        }

    }
    return this.obj.format();
}

if(window.template){
    window.template.helper('dateFormat', _dateFormat);
    window.template.helper('tabKeyWord', function(date, key){
        if(date && key){
            //不区分大小写匹配关键字输出
            let reg = new RegExp(key, "gi");
            date = date.replace(reg, function(txt){
                return "<code>"+txt+"</code>";
            })
            return date;
        }
        return date;
    });
}

$(document).ready(function () {
    let _menus = $("#wrapper");
    _app.ajaxHrefEle(_menus);
    _app.ajaxContentEle(_menus);
});

