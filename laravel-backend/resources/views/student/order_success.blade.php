<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Order Confirmed | ATU Cafeteria Mobile Order</title>
    <!-- Tailwind CSS CDN -->
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
    <style>
        body {
            font-family: 'Plus Jakarta Sans', sans-serif;
        }
    </style>
</head>
<body class="bg-slate-50 min-h-screen text-slate-800 flex flex-col justify-between pb-8">

    <main class="flex-grow max-w-md mx-auto w-full px-4 mt-12 flex flex-col items-center justify-center">

        <!-- Animated/Polished Success Card -->
        <div class="bg-white rounded-3xl border border-slate-200 shadow-xl overflow-hidden w-full p-8 text-center flex flex-col items-center">
            
            <!-- Beautiful animated green status circle -->
            <div class="h-20 w-20 bg-emerald-50 text-emerald-500 rounded-full flex items-center justify-center border-4 border-emerald-100 shadow-sm mb-6 animate-bounce">
                <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"></path>
                </svg>
            </div>

            <span class="px-3 py-1 bg-emerald-50 text-emerald-700 text-xs font-bold rounded-lg mb-1 tracking-wide uppercase">
                Order Sent to Kitchen
            </span>
            <h2 class="text-2xl font-extrabold text-slate-900 mb-2">Checkout Complete!</h2>
            <p class="text-xs text-slate-400 mb-6 font-medium">Order ID: <b class="code-font text-indigo-600 font-bold">#{{ $order->id }}</b> &bull; Placed at {{ date('H:i') }}</p>

            <!-- PIN Display Frame -->
            <div class="bg-gradient-to-br from-indigo-900 to-slate-900 text-white rounded-3xl p-6 w-full shadow-lg mb-6 border border-indigo-950 relative overflow-hidden">
                <!-- Background visual touch -->
                <div class="absolute right-0 top-0 text-slate-700 opacity-20 text-7xl translate-x-4 -translate-y-4">🔑</div>
                
                <p class="text-[10px] text-indigo-300 font-bold uppercase tracking-widest mb-1.5">Secure Pickup Code PIN</p>
                <p class="text-5xl font-black tracking-widest code-font text-yellow-300 drop-shadow mb-2">{{ $order->pickup_pin }}</p>
                <div class="h-px bg-indigo-800/60 my-3"></div>
                <p class="text-xs text-indigo-200 font-medium">Present this 4-digit code at the cafeteria booth to verify and fetch your meal.</p>
            </div>

            <!-- Receipt Breakdown -->
            <div class="w-full text-left bg-slate-50 border border-slate-200 rounded-2xl p-5 mb-6 space-y-3">
                <h4 class="text-xs font-extrabold text-slate-400 tracking-wider uppercase mb-1">Receipt Summary</h4>
                
                <div class="flex items-center justify-between text-sm">
                    <span class="text-slate-600">Item ordered:</span>
                    <strong class="text-slate-900">{{ $item->name }}</strong>
                </div>

                <div class="flex items-center justify-between text-sm">
                    <span class="text-slate-600">Quantity:</span>
                    <strong class="text-slate-900 font-bold">{{ $order->quantity }}x</strong>
                </div>

                <div class="flex items-center justify-between text-sm">
                    <span class="text-slate-600">Vendor Provider:</span>
                    <strong class="text-slate-700">{{ $item->vendor->profile_info['outlet_name'] ?? $item->vendor->fullName }}</strong>
                </div>

                <div class="flex items-center justify-between text-sm">
                    <span class="text-slate-600">Estimated pickup:</span>
                    <strong class="text-indigo-600 font-bold">{{ $order->estimated_pickup_time }}</strong>
                </div>

                <div class="h-px bg-slate-200 my-2"></div>

                <div class="flex items-center justify-between">
                    <span class="text-xs text-slate-400 font-bold uppercase tracking-wider">Amount Paid</span>
                    <strong class="text-lg font-black text-slate-900 code-font">GH₵ {{ number_format($order->total_price, 2) }}</strong>
                </div>
            </div>

            <!-- Customer Details Info -->
            <div class="text-xs text-slate-500 mb-2 flex items-center gap-1.5 justify-center">
                <span>👤 Student Account:</span>
                <strong class="text-slate-800">{{ $student->fullName }} ({{ $student->student_staff_id }})</strong>
            </div>

        </div>

        <div class="mt-8 flex flex-col items-center gap-3 w-full">
            <a href="/api/student/order-item/{{ $item->id }}" class="w-full py-4 bg-slate-800 hover:bg-slate-900 text-white font-bold rounded-2xl text-center shadow-md transition-all text-sm flex items-center justify-center gap-2" style="min-height: 48px;">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.21 8H18.2M7 9a7 7 0 1111.9 4.9L15 17H9l-2-2"></path>
                </svg>
                <span>Order Again</span>
            </a>
        </div>

    </main>

    <footer class="max-w-md mx-auto text-center text-[10px] text-slate-400 px-4 mt-6">
        <p>&copy; 2026 Accra Technical University Cafeteria Network</p>
    </footer>

</body>
</html>
