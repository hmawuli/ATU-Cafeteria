<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Order {{ $item->name }} | ATU Cafeteria Mobile Order</title>
    <!-- Tailwind CSS CDN -->
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
    <style>
        body {
            font-family: 'Plus Jakarta Sans', sans-serif;
        }
    </style>
</head>
<body class="bg-slate-50 min-h-screen text-slate-800 flex flex-col pb-8">

    <!-- Top Navigation Bar -->
    <header class="bg-gradient-to-r from-indigo-900 to-indigo-800 text-white shadow-md sticky top-0 z-50">
        <div class="max-w-md mx-auto px-4 py-4 flex items-center justify-between">
            <div class="flex items-center gap-2">
                <div class="bg-indigo-600 p-1.5 rounded-lg">
                    <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"></path>
                    </svg>
                </div>
                <span class="font-black tracking-tight text-sm uppercase">ATU Cafeteria</span>
            </div>
            <span class="text-[10px] bg-emerald-500/20 text-emerald-300 font-bold px-2.5 py-1 rounded-full border border-emerald-500/30">
                Active Menu Scanner
            </span>
        </div>
    </header>

    <main class="flex-grow max-w-md mx-auto w-full px-4 mt-6">

        <!-- Food Item Card Banner -->
        <div class="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden mb-6">
            <div class="bg-indigo-50 p-6 flex flex-col items-center justify-center text-center relative">
                <!-- Large decorative visual food icon -->
                <div class="text-6xl mb-3 filter drop-shadow">🍛</div>
                <span class="px-2.5 py-0.5 bg-indigo-100 text-indigo-800 text-[10px] font-bold rounded-full uppercase tracking-wider mb-2">
                    {{ $item->category }}
                </span>
                <h2 class="text-2xl font-extrabold text-slate-900 leading-tight mb-1">{{ $item->name }}</h2>
                <p class="text-xs text-slate-500">Prepared with love by <strong class="text-indigo-600">{{ $item->vendor->fullName }}</strong></p>
                <div class="absolute right-4 bottom-4 bg-white/90 backdrop-blur border border-slate-200 shadow-xs px-3 py-1 rounded-full text-[11px] text-slate-500 flex items-center gap-1">
                    📍 <span>{{ $item->vendor->profile_info['location'] ?? 'Cafeteria' }}</span>
                </div>
            </div>

            <div class="p-6 border-t border-slate-100">
                <p class="text-xs text-slate-400 font-extrabold uppercase tracking-wider mb-1.5">Dish Description</p>
                <p class="text-slate-600 text-sm leading-relaxed">
                    {{ $item->description }}
                </p>
            </div>
        </div>

        <!-- Placing Order Form -->
        <div class="bg-white rounded-3xl border border-slate-200 shadow-sm p-6">
            <h3 class="text-lg font-extrabold text-slate-900 mb-4">🛒 Configure Your Hot Meal</h3>

            <form action="/api/student/order-item/{{ $item->id }}/place" method="POST" id="orderForm" class="space-y-5">
                @csrf

                <!-- Quantity selector -->
                <div>
                    <label class="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Select Quantity</label>
                    <div class="flex items-center gap-4">
                        <div class="flex items-center border border-slate-300 rounded-2xl overflow-hidden shadow-xs bg-slate-50">
                            <button type="button" onclick="adjustQty(-1)" class="w-12 h-12 flex items-center justify-center text-slate-600 font-bold hover:bg-slate-200 active:bg-slate-300 transition-colors text-lg focus:outline-none" style="min-width: 48px; min-height: 48px;">
                                −
                            </button>
                            <input type="number" id="quantity" name="quantity" value="1" min="1" max="10" readonly class="w-12 text-center bg-transparent font-black text-slate-800 text-lg outline-none">
                            <button type="button" onclick="adjustQty(1)" class="w-12 h-12 flex items-center justify-center text-slate-600 font-bold hover:bg-slate-200 active:bg-slate-300 transition-colors text-lg focus:outline-none" style="min-width: 48px; min-height: 48px;">
                                +
                            </button>
                        </div>
                        <div class="flex-grow text-right">
                            <p class="text-xs text-slate-400 font-semibold uppercase">Unit Price</p>
                            <p class="text-base font-bold text-slate-700">GH₵ {{ number_format($item->price, 2) }}</p>
                        </div>
                    </div>
                </div>

                <!-- Select Student Account -->
                <div>
                    <label for="student_id" class="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1.5">Your Student Account</label>
                    <div class="relative">
                        <select name="student_id" id="student_id" required class="w-full pl-4 pr-10 py-3 text-sm rounded-xl border border-slate-300 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all bg-white text-slate-700 shadow-xs appearance-none cursor-pointer">
                            @foreach($students as $student)
                                <option value="{{ $student->id }}">
                                    {{ $student->fullName }} (ID: {{ $student->student_staff_id }})
                                </option>
                            @endforeach
                        </select>
                        <div class="absolute inset-y-0 right-3 flex items-center pointer-events-none text-slate-400">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
                            </svg>
                        </div>
                    </div>
                    <p class="text-[10px] text-slate-400 mt-1 font-medium">Select your seeded account to process simulated wallet deductions.</p>
                </div>

                <!-- Pickup Time Selector -->
                <div>
                    <label for="pickup_time" class="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1.5">Estimated Pickup Time</label>
                    <div class="relative">
                        <select name="pickup_time" id="pickup_time" required class="w-full pl-4 pr-10 py-3 text-sm rounded-xl border border-slate-300 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all bg-white text-slate-700 shadow-xs appearance-none cursor-pointer">
                            <option value="In 10 minutes">⚡ ASAP (In 10 minutes)</option>
                            <option value="In 20 minutes" selected>In 20 minutes</option>
                            <option value="In 30 minutes">In 30 minutes</option>
                            <option value="In 45 minutes">In 45 minutes</option>
                            <option value="In 1 hour">In 1 hour</option>
                        </select>
                        <div class="absolute inset-y-0 right-3 flex items-center pointer-events-none text-slate-400">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
                            </svg>
                        </div>
                    </div>
                </div>

                <!-- Total cost preview & submit button -->
                <div class="pt-4 border-t border-slate-100 flex items-center justify-between">
                    <div>
                        <p class="text-xs text-slate-400 font-extrabold uppercase tracking-wider">Total Price</p>
                        <p class="text-2xl font-black text-slate-900 code-font">GH₵ <span id="totalDisplay">{{ number_format($item->price, 2) }}</span></p>
                    </div>

                    <button type="submit" class="px-6 py-3.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-2xl transition-all shadow-md hover:shadow-lg flex items-center gap-2 transform active:scale-95 text-sm" style="min-height: 48px;">
                        <span>Confirm Order</span>
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                        </svg>
                    </button>
                </div>
            </form>
        </div>

    </main>

    <!-- Simple JS interactive logic -->
    <script>
        const unitPrice = {{ $item->price }};
        const qtyInput = document.getElementById('quantity');
        const totalDisplay = document.getElementById('totalDisplay');

        function adjustQty(amount) {
            let current = parseInt(qtyInput.value);
            current += amount;
            if (current < 1) current = 1;
            if (current > 10) current = 10;
            qtyInput.value = current;
            
            // Recalculate cost
            const total = (unitPrice * current).toFixed(2);
            totalDisplay.textContent = total;
        }
    </script>
</body>
</html>
