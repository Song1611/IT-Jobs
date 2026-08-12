package com.itjob.mapper;

import com.itjob.dto.response.AttachmentResponse;
import com.itjob.dto.response.PostResponse;
import com.itjob.entity.Attachment;
import com.itjob.entity.Post;
import com.itjob.mapper.config.CentralMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class, uses = {UserMapper.class, CompanyMapper.class})
public interface PostMapper {

    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "interaction", ignore = true)
    PostResponse toPostResponse(Post post);

    AttachmentResponse toAttachmentResponse(Attachment attachment);
}
