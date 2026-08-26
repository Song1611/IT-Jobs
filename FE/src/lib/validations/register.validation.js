import { z } from "zod";

// Schema cho đăng ký ứng viên (user) - khớp RegisterRequest BE
export const RegisterFormSchema = z.object({
  fullName: z.string().min(2, { message: "Họ tên ít nhất 2 ký tự" }),
  email: z.string().email({ message: "Email không hợp lệ" }),
  password: z.string().min(6, { message: "Mật khẩu ít nhất 6 ký tự" })
});



// Schema cho đăng ký nhà tuyển dụng (HR) - khớp RegisterRequest BE
export const RegisterHRFormSchema = z.object({
  fullName: z.string().min(2, { message: "Họ tên ít nhất 2 ký tự" }),
  email: z.string().email({ message: "Email không hợp lệ" }),
  password: z.string().min(6, { message: "Mật khẩu ít nhất 6 ký tự" })
});