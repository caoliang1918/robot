$(document).ready(function () {

    var _dateFormat = function(date){
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
            return this.y + '-' + this.m + '-' + this.d+' '+this.h+':'+this.minute+':'+this.second+':'+this.milliseconds;
        }
        return this.obj.format();
    }

    template.helper('dateFormat', _dateFormat);

    window._sidebar = function(document, renderJsId, mustUrl, title){
        if(document){
            var eles = document.find(".toggle-sidebar");
            if(eles){
                eles.each(function(){
                    let ele = $(this);
                    let durl = ele.attr("durl");
                    if(mustUrl && !durl){
                        console.log("ele ducment class is: "+document.attr("class")+" . AND ele class is : "+ele.attr("class")+" MISS toggle-sidebar ele durl. the ele is remove().")
                        ele.remove();
                        return ;
                    }
                    if(durl){
                        ele.removeAttr("durl");
                    }
                    let dparams = ele.attr("dparams");
                    ele.click(function () {
                        if(durl){
                            let _data = _ajax(durl, dparams);
                            let param = {data: _data};
                            if(title){
                                param.title = title;
                            }
                            let _html = template(renderJsId, param);
                            let _model = $('.sidebar-change:first .candidateSidebar');
                            _model.html(_html);
                            _sidebar(_model.find(".modal-header"));
                        }
                        $("#sidebar").toggleClass("collapsed");
                        return false;
                    });
                })
            }
        }
    }

    window._ajax = function(url, params){
        var _data = {};
        if(url){
            if(!params){
                params = {};
            }
            $.ajax({ url: url
                , data: params
                , dataType: 'json'
                , async: false
                , success: function(data){
                    if(data.error_code == "0"){
                        console.log(data)
                        _data = data.result;
                    }
                }
                , error: function(data){
                    console.log(data);
                }});
        }
        return _data;
    }

    window._page = function(params, document, hasSearchPageFun, renderJsId, modelsFunc){
        if(!renderJsId){
            console.log("miss page renderJsId error!");
            return ;
        }
        var obj = new Object();
        obj.renderJsId = renderJsId;
        obj.table = document.find("table");
        if(!obj.table){
            console.log("miss page table error!");
            return ;
        }
        obj.url = obj.table.attr("durl");
        if(!obj.url){
            console.log("miss page url error!");
            return ;
        }
        obj.table.removeAttr("durl");
        obj.params = params;
        obj.tbody = document.find("table tbody");
        obj.code = document.find("table code");
        obj.pager = document.find("nav ul.pager");
        obj.btnSearch = document.find("button.btn-page-num-search");
        obj.inputSearch = document.find("input.input-page-num-search");
        obj.hasSearchPageFun = hasSearchPageFun;
        if(obj.hasSearchPageFun){
            obj.searchForm = document.find("form.option-search-form:first");
        }
        obj.modelsFunc = modelsFunc;
        obj.page = function(){
            if(this.url){
                if(!this.params){
                    this.params = {};
                }
                let $obj = this;
                $.ajax({ url: this.url
                    , data: this.params
                    , dataType: 'json'
                    , success: function(data){
                        console.log(data);
                        $obj.data = data;
                        $obj.makePageView();
                        $obj.doModelsFunc()
                    }
                    , error: function(data){
                        console.log(data);
                    }});
            }
        };
        obj.makePageView = function () {
            if(this.data && this.data.error_code == "0"){
                let result = this.data.result;
                if(result && this.tbody && this.code && this.pager){
                    if(result.result.length == 0){
                        this.code.html(result.pageNumber);
                        this.params.p = result.pageNumber;
                        this.tbody.empty();
                        this.makePager(result.pageNumber, true);
                    }else{
                        this.code.html(result.pageNumber);
                        let startNum = result.pageNumber > 1 ? (result.pageNumber - 1) * result.pageSize : 0;
                        this.tbody.empty();
                        $(template(this.renderJsId, {data:result.result, startNum:startNum})).appendTo(this.tbody);
                        this.makePager(result.pageNumber, result.result.length < result.pageSize?true:false);
                    }
                }
            }
        };

        obj.makePager = function(pageNumber, isEmd){
            let prevHtml = this.pager.find("a:first");
            let nextHtml = this.pager.find("a:last");
            prevHtml.unbind("click");
            nextHtml.unbind("click");
            let $obj = this;
            if(isEmd){
                prevHtml.click(function(){
                    $obj.params.p = pageNumber - 1;
                    $obj.page();
                });
                nextHtml.click(function(){
                    alert("已到最后页");
                });
            }else if(pageNumber <= 1){
                prevHtml.click(function(){
                    alert("已到最前页");
                });
                nextHtml.click(function(){
                    $obj.params.p = 2;
                    $obj.page();
                });
            }else{
                prevHtml.click(function(){
                    $obj.params.p = pageNumber - 1;
                    $obj.page();
                });
                nextHtml.click(function(){
                    $obj.params.p = pageNumber + 1;
                    $obj.page();
                });
            }
        };
        obj.search = function(){
            if(this.btnSearch && this.inputSearch){
                let $obj = this;
                this.btnSearch.click(function(){
                    if($obj.inputSearch.val() != ""){
                        $obj.params.p = $obj.inputSearch.val();
                        $obj.page();
                        $obj.inputSearch.val("");
                    }
                })
            }
            return this;
        };

        obj.searchOption = function(){
            if(this.searchForm){
                let $searchForm = this.searchForm;
                let $obj = this;
                let searchBtnOption = $searchForm.find("button.btn-page-option-search");
                if(searchBtnOption){
                    searchBtnOption.click(function(){
                        let _params = $searchForm.serializeJSON();
                        if(_params){
                            let _p = $obj.params.p;
                            if(_p == undefined || _p <= 1){
                                _params.p = 1;
                            }
                            $obj.params = _params;
                            $obj.page();
                        }
                    });
                }
                let resetBtnOption = $searchForm.find("button.btn-page-option-reset-search")
                if(resetBtnOption){
                    resetBtnOption.click(function(){
                        $(this).parents("form:first").find(".btn-page-option-ele").val("");
                        $obj.params = {p:1};
                        $obj.page();
                    });
                }
            }
            return this;
        };
        obj.doModelsFunc = function(){
            if(this.modelsFunc){
                this.modelsFunc(this.tbody);
            }
            return this;
        }
        return obj.search().searchOption();
    }
});