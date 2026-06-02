import { useState } from 'react'
import axios from 'axios'
import { useNavigate } from 'react-router-dom'
import { PATH } from 'src/constants/path'
import {
  cardBadge,
  cardDescription,
  cardStyle,
  cardTitle,
  containerStyle,
  helperLink,
  inputStyle,
  loginButton,
} from 'src/styles/js/auth/LoginStyle'

const initialForm = {
  empId: '',
  password: '',
  phone: '',
  email: '',
}

function AdminSignupPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState(initialForm)
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const handleChange = (event) => {
    setForm((current) => ({
      ...current,
      [event.target.name]: event.target.value,
    }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setIsLoading(true)

    try {
      const response = await axios.post(`${PATH.API.BASE}${PATH.API.ADMIN_SIGNUP}`, form)
      alert(`관리자 회원가입이 완료되었습니다.\n발급된 사번: ${response.data.empNo}\n로그인해 주세요.`)
      navigate(PATH.AUTH.LOGIN)
    } catch (err) {
      const message = err.response?.data
      setError(typeof message === 'string' ? message : '회원가입 중 오류가 발생했습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div style={containerStyle}>
      <div style={{ ...cardStyle, maxWidth: '520px' }}>
        <div style={cardBadge}>ADMIN SIGN UP</div>
        <h2 style={cardTitle}>관리자 회원가입</h2>
        <p style={cardDescription}>관리자 계정으로 사용할 정보를 입력해 주세요.</p>

        <form onSubmit={handleSubmit}>
          <input value="가입 시 자동으로 부여됩니다." style={inputStyle} disabled />
          <input name="empId" placeholder="아이디" value={form.empId} onChange={handleChange} style={inputStyle} required />
          <input
            type="password"
            name="password"
            placeholder="비밀번호"
            value={form.password}
            onChange={handleChange}
            style={inputStyle}
            required
          />
          <input name="phone" placeholder="연락처" value={form.phone} onChange={handleChange} style={inputStyle} required />
          <input
            type="email"
            name="email"
            placeholder="이메일"
            value={form.email}
            onChange={handleChange}
            style={inputStyle}
            required
          />

          {error && <p style={{ color: '#d93025', fontSize: '14px', marginBottom: '16px' }}>{error}</p>}
          <button type="submit" style={loginButton(isLoading)} disabled={isLoading}>
            {isLoading ? '가입 처리 중...' : '관리자 회원가입'}
          </button>
        </form>

        <div style={{ marginTop: '22px', textAlign: 'center' }}>
          <span onClick={() => navigate(PATH.AUTH.LOGIN)} style={helperLink}>
            로그인 화면으로 돌아가기
          </span>
        </div>
      </div>
    </div>
  )
}

export default AdminSignupPage
