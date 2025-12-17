import axios from 'axios'

const baseURL = import.meta.env.VITE_API_URL || 'http://localhost:3000'

const instance = axios.create({ baseURL, timeout: 5000 })

export default instance