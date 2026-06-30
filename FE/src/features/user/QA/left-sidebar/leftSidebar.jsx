"use client";

import ProfileSection from "@/features/user/QA/left-sidebar/profile.section";
import FriendFollow from "@/features/user/QA/right-sidebar/friend-follow.section";
import { FollowListProps } from "@/types/follow.type";

export default function LeftSidebar({ followList }) {
  return (
    <div className="flex flex-col gap-4">
      {/* Profile Card - Fixed size */}
      <div className="flex-shrink-0">
        <ProfileSection />
      </div>
      
      {/* Friends List - Takes remaining space */}
      <div className="flex-1">
        <FriendFollow friendList={followList} />
      </div>
    </div>);

}