package net.kaciras.blog.api.discuss;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface DiscussMapper {

	DiscussionVo toView(Discussion items);
}
