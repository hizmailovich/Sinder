package com.solvd.laba.sinder.web.dto.mapper;

import com.solvd.laba.sinder.domain.chat.Attachment;
import com.solvd.laba.sinder.web.dto.AttachmentDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {

    Attachment toEntity(AttachmentDto dto);

    AttachmentDto toDto(Attachment entity);

}
