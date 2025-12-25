import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '@tamagui/core/reset.css'
import './index.css'
import App from './App.tsx'
import {Providers} from "./providers.tsx";
import {
  createBrowserRouter,
  RouterProvider,
} from 'react-router-dom'

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
  },
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Providers>
      <RouterProvider router={router} />
    </Providers>
  </StrictMode>,
)
