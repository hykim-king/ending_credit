//문자열 null check
const isEmpty = (input,message) => {
    if(input.value.trim() === ''){
        alert(message);
        input.focus();
        return true;
    }

    return false;
};

//숫자 check
const isNumber = (input, message) => {
    if (Number.isNaN(Number(input.value.trim()))) {
      alert(message);
      input.focus();
      return false;
    }

    return true;
};

//email
  const isValidEmail = (input) => {
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!emailPattern.test(input.value.trim())) {
      alert('올바른 이메일 형식이 아닙니다.');
      input.focus();
      return false;
    }

    return true;
  };