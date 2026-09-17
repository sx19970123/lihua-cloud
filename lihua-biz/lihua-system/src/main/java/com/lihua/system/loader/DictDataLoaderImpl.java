package com.lihua.system.loader;

import com.lihua.dict.loader.DictDataLoader;
import com.lihua.dict.model.DictDataModel;
import com.lihua.system.mapper.SysDictDataMapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 字典缓存回源实现：sys_dict_data 表 Owner 提供，@ComponentScan 自动注册为 DictUtils 回源通道。
 * @Primary：system 进程同时引入 api-system 的 RPC 版实现（SysDictDataClientFacade），本地直调优先
 */
@Component
@Primary
public class DictDataLoaderImpl implements DictDataLoader {

    @Resource
    private SysDictDataMapper sysDictDataMapper;

    @Override
    public List<DictDataModel> queryByDictTypeCode(List<String> dictTypeCodeList) {
        return sysDictDataMapper.queryByDictTypeCode(dictTypeCodeList);
    }
}
