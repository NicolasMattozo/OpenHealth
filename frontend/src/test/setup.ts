import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Sem "globals", o Testing Library nao desmonta os componentes entre os testes sozinho.
afterEach(() => cleanup())
