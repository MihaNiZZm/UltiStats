package com.github.mihanizzm.ultistats.config

import com.github.mihanizzm.ultistats.dto.common.SortParam
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class SortParamConverter : Converter<String, SortParam> {
    override fun convert(source: String): SortParam = SortParam.parse(source)
}
