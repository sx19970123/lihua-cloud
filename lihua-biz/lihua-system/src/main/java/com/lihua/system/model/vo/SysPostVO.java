package com.lihua.system.model.vo;

import com.lihua.system.entity.SysPost;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.fesod.sheet.annotation.write.style.ColumnWidth;

@ExcelIgnoreUnannotated
@EqualsAndHashCode(callSuper = true)
@Data
public class SysPostVO extends SysPost {

    // 所属部门名称
    @ExcelProperty("所属部门")
    @ColumnWidth(20)
    private String deptName;

    // 持有该岗位的用户id（用户模块列表/导出按用户归并岗位时使用）
    private String userId;
}
