"use client";

import { useState } from "react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription } from
"@/components/ui/shadcn/card";
import { Button } from "@/components/ui/shadcn/button";
import { Input } from "@/components/ui/shadcn/input";
import { Label } from "@/components/ui/shadcn/label";
import { useAuth } from "@/components/providers/auth.provider";
import { userApi } from "@/services/user.api";
import { toast } from "sonner";

export default function SettingsPage() {
  const { user, token } = useAuth();

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [otp, setOtp] = useState("");
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [isSendingOtp, setIsSendingOtp] = useState(false);

  const handleSendOtp = async () => {
    if (!user?.id || !token) {
      toast.error("Vui lòng đăng nhập lại");
      return;
    }

    setIsSendingOtp(true);
    try {
      await userApi.sendChangePasswordOtp(user.id);
      toast.success("Mã OTP đã được gửi vào email của bạn");
    } catch (error) {
      toast.error(error.message || "Gửi OTP thất bại");
    } finally {
      setIsSendingOtp(false);
    }
  };

  const handleChangePassword = async () => {
    // Validation
    if (!currentPassword || !newPassword || !confirmPassword || !otp) {
      toast.error("Vui lòng điền đầy đủ thông tin");
      return;
    }

    if (newPassword !== confirmPassword) {
      toast.error("Mật khẩu mới và xác nhận mật khẩu không khớp");
      return;
    }

    if (newPassword.length < 6) {
      toast.error("Mật khẩu mới phải có ít nhất 6 ký tự");
      return;
    }

    if (!user?.id || !token) {
      toast.error("Vui lòng đăng nhập lại");
      return;
    }

    setIsChangingPassword(true);

    try {
      const response = await userApi.changePassword(
        user.id,
        currentPassword,
        newPassword,
        otp
      );

      toast.success(response.message || "Đổi mật khẩu thành công");

      // Reset form
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setOtp("");
    } catch (error) {
      toast.error(error.message || "Đổi mật khẩu thất bại");
    } finally {
      setIsChangingPassword(false);
    }
  };

  return (
    <div className="container mx-auto px-4 py-8 max-w-3xl">
      <h1 className="text-3xl font-bold mb-6">Cài đặt tài khoản</h1>

      <div className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>Thông tin cá nhân</CardTitle>
            <CardDescription>Cập nhật thông tin cơ bản của bạn</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Họ và tên</Label>
              <Input id="name" defaultValue={user?.fullName} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                defaultValue={user?.email}
                disabled />
              
            </div>
            <Button>Lưu thay đổi</Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Đổi mật khẩu</CardTitle>
            <CardDescription>Cập nhật mật khẩu của bạn</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="current-password">Mật khẩu hiện tại</Label>
              <Input
                id="current-password"
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)} />
              
            </div>
            <div className="space-y-2">
              <Label htmlFor="new-password">Mật khẩu mới</Label>
              <Input
                id="new-password"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)} />
              
            </div>
            <div className="space-y-2">
              <Label htmlFor="confirm-password">Xác nhận mật khẩu</Label>
              <Input
                id="confirm-password"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)} />
              
            </div>
            <div className="space-y-2">
              <Label htmlFor="otp">Mã OTP</Label>
              <div className="flex gap-2">
                <Input
                  id="otp"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  placeholder="Nhập mã OTP từ email" />
                <Button
                  variant="outline"
                  onClick={handleSendOtp}
                  disabled={isSendingOtp}
                  className="shrink-0">
                  {isSendingOtp ? "Đang gửi..." : "Gửi OTP"}
                </Button>
              </div>
            </div>
            <Button onClick={handleChangePassword} disabled={isChangingPassword}>
              {isChangingPassword ? "Đang xử lý..." : "Đổi mật khẩu"}
            </Button>
          </CardContent>
        </Card>

        <Card className="border-destructive">
          <CardHeader>
            <CardTitle className="text-destructive">Xoá tài khoản</CardTitle>
            <CardDescription>
              Hành động này không thể hoàn tác. Tất cả dữ liệu của bạn sẽ bị xoá
              vĩnh viễn.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button variant="destructive">Xoá tài khoản</Button>
          </CardContent>
        </Card>
      </div>
    </div>);

}