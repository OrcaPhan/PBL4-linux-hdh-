# Frontend - React + TypeScript

Modern UI cho ORCA System Monitor.

Dự án gốc từ Figma: https://www.figma.com/design/SJ6glrvEWjX8HdeoiAfSBo/Memory-Management-App-Design

## Công nghệ

- React 18
- TypeScript
- Vite
- shadcn/ui
- Recharts (biểu đồ)
- TailwindCSS

## Development

```bash
# Cài đặt dependencies
npm install

# Chạy dev server
npm run dev
```

Frontend sẽ chạy tại `http://localhost:5173`

## Build

```bash
# Build production
npm run build

# Preview production build
npm run preview
```

## Build Desktop App (Tauri)

```bash
# Cài đặt Tauri CLI
npm install -D @tauri-apps/cli

# Dev mode
npm run tauri dev

# Build
npm run tauri build
```

## Cấu trúc

```
src/
├── components/
│   ├── MemoryChart.tsx       # Biểu đồ memory
│   ├── MemoryOverview.tsx    # Tổng quan memory
│   ├── ProcessTable.tsx      # Bảng processes
│   └── ui/                   # shadcn/ui components
├── services/
│   └── api.ts                # API client
├── styles/
│   └── globals.css           # Global styles
├── App.tsx                   # Main app component
└── main.tsx                  # Entry point
```

## Environment Variables

Tạo file `.env` nếu cần:

```env
VITE_API_URL=http://localhost:8080
```
