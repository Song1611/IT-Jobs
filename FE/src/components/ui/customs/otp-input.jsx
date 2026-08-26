import React, { useRef, useState, useEffect } from "react";
import { cn } from "@/lib/utils";

export function OTPInput({ length = 6, value, onChange, className }) {
  const [otp, setOtp] = useState(Array(length).fill(""));
  const inputRefs = useRef([]);

  useEffect(() => {
    if (value === "") {
      setOtp(Array(length).fill(""));
    } else if (value && value.length <= length) {
      const newOtp = Array(length).fill("");
      value.split("").forEach((char, i) => {
        newOtp[i] = char;
      });
      setOtp(newOtp);
    }
  }, [value, length]);

  const handleChange = (e, index) => {
    const text = e.target.value;
    if (!/^[0-9]*$/.test(text)) return;
    
    // Only take the last character typed
    const newOtp = [...otp];
    const newChar = text.substring(text.length - 1);
    newOtp[index] = newChar;
    setOtp(newOtp);
    onChange(newOtp.join(""));
    
    // Move to next input
    if (newChar && index < length - 1) {
      inputRefs.current[index + 1].focus();
    }
  };

  const handleKeyDown = (e, index) => {
    if (e.key === "Backspace") {
      if (!otp[index] && index > 0) {
        inputRefs.current[index - 1].focus();
      } else {
        // Just let it clear current character
        const newOtp = [...otp];
        newOtp[index] = "";
        setOtp(newOtp);
        onChange(newOtp.join(""));
      }
    } else if (e.key === "ArrowLeft" && index > 0) {
      inputRefs.current[index - 1].focus();
    } else if (e.key === "ArrowRight" && index < length - 1) {
      inputRefs.current[index + 1].focus();
    }
  };
  
  const handlePaste = (e) => {
    e.preventDefault();
    const pastedData = e.clipboardData.getData("text").slice(0, length).split("");
    if (pastedData.some(char => !/^[0-9]$/.test(char))) return;
    
    const newOtp = [...otp];
    pastedData.forEach((char, i) => {
      newOtp[i] = char;
    });
    setOtp(newOtp);
    onChange(newOtp.join(""));
    
    // Focus the next empty input or the last one
    const nextEmptyIndex = newOtp.findIndex(val => !val);
    if (nextEmptyIndex !== -1) {
      inputRefs.current[nextEmptyIndex].focus();
    } else {
      inputRefs.current[length - 1].focus();
    }
  };

  return (
    <div className={cn("flex gap-2 justify-center items-center", className)}>
      {otp.map((digit, index) => (
        <input
          key={index}
          ref={(ref) => (inputRefs.current[index] = ref)}
          type="text"
          inputMode="numeric"
          autoComplete="one-time-code"
          pattern="\d{1}"
          maxLength={1}
          value={digit}
          onChange={(e) => handleChange(e, index)}
          onKeyDown={(e) => handleKeyDown(e, index)}
          onPaste={handlePaste}
          className="w-11 h-14 sm:w-12 sm:h-14 text-center text-xl sm:text-2xl font-semibold border border-input rounded-xl focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all bg-background text-foreground shadow-sm"
        />
      ))}
    </div>
  );
}
