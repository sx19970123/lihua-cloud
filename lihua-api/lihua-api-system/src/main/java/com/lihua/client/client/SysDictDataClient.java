package com.lihua.client.client;

import com.lihua.client.annotation.RemoteClient;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.dict.model.DictDataModel;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

@RemoteClient(serverName = "lihua-system")
@HttpExchange("system/dictData")
public interface SysDictDataClient {

    /**
     * 按字典类型编码批量查询生效字典数据（缓存回源通道）
     */
    @PostExchange("queryByDictTypeCode")
    ApiResponseModel<List<DictDataModel>> queryByDictTypeCode(@RequestBody List<String> dictTypeCodeList);
}
