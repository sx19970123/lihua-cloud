package com.lihua.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lihua.file.entity.SysAttachment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysAttachmentMapper extends BaseMapper<SysAttachment> {

    List<String> queryDeletablePathByIds(@Param("ids") List<String> ids);

}
