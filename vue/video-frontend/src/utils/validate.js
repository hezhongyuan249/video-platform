// 通用表单校验规则
/**
 * 密码校验规则（8位以上+大小写+数字+特殊字符）
 * @param {Object} rule 校验规则
 * @param {String} value 密码值
 * @param {Function} callback 回调函数
 */
export const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入密码'))
  } else if (value.length < 8) {
    callback(new Error('密码长度不能少于8位'))
  } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\da-zA-Z])/.test(value)) {
    callback(new Error('密码必须包含大小写字母、数字、特殊字符'))
  } else {
    callback()
  }
}

/**
 * 确认密码校验
 * @param {String} password 原密码
 * @returns {Function} 校验函数
 */
export const validateConfirmPwd = (password) => {
  return (rule, value, callback) => {
    if (!value) {
      callback(new Error('请确认密码'))
    } else if (value !== password) {
      callback(new Error('两次输入的密码不一致'))
    } else {
      callback()
    }
  }
}