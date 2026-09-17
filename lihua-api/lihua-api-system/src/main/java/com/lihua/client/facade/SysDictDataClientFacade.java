package com.lihua.client.facade;

import com.lihua.client.client.SysDictDataClient;
import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.exception.ServiceException;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.dict.loader.DictDataLoader;
import com.lihua.dict.model.DictDataModel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 字典缓存回源的 RPC 通道（无 sys_dict_data 表的服务消费；system 本地实现 @Primary 优先）
 */
@Component
@Slf4j
public class SysDictDataClientFacade implements DictDataLoader {

    @Resource
    private SysDictDataClient sysDictDataClient;

    /**
     * 按字典类型编码批量查询生效字典数据
     */
    @Override
    @CircuitBreaker(name = "sysDictData")
    public List<DictDataModel> queryByDictTypeCode(List<String> dictTypeCodeList) {
        ApiResponseModel<List<DictDataModel>> responseModel = sysDictDataClient.queryByDictTypeCode(dictTypeCodeList);
        if (!ResultCodeEnum.SUCCESS.getCode().equals(responseModel.getCode())) {
            throw new ServiceException("字典数据查询失败：" + responseModel.getMsg());
        }
        return responseModel.getData();
    }
}
