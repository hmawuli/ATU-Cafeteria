<?php

namespace App\Exceptions;

use Illuminate\Foundation\Exceptions\Handler as ExceptionHandler;
use Throwable;
use Illuminate\Database\QueryException;
use Illuminate\Support\Facades\Log;

class Handler extends ExceptionHandler
{
    /**
     * A list of exception types with their corresponding custom log levels.
     *
     * @var array<class-string<Throwable>, \Psr\Log\LogLevel::*>
     */
    protected $levels = [
        //
    ];

    /**
     * A list of the exception types that are not reported.
     *
     * @var array<int, class-string<Throwable>>
     */
    protected $dontReport = [
        //
    ];

    /**
     * A list of the inputs that are never flashed to the session on validation exceptions.
     *
     * @var array<int, string>
     */
    protected $dontFlash = [
        'current_password',
        'password',
        'password_confirmation',
    ];

    /**
     * Register the exception handling callbacks for the application.
     */
    public function register(): void
    {
        $this->reportable(function (Throwable $e) {
            //
        });

        // Capture Database Query Failures globally
        $this->renderable(function (QueryException $e, $request) {
            Log::error('Database query failure captured globally: ' . $e->getMessage(), [
                'sql' => $e->getSql(),
                'bindings' => $e->getBindings(),
                'trace' => $e->getTraceAsString(),
            ]);

            if ($request->is('api/*') || $request->expectsJson() || $request->ajax()) {
                return response()->json([
                    'success' => false,
                    'message' => 'A database query error occurred. Please try again later.',
                    'error' => config('app.debug') ? $e->getMessage() : 'Database Error'
                ], 500);
            }

            return redirect()->back()
                ->withInput()
                ->with('error', 'A database transaction or query failure occurred. Please review your input or try again later.');
        });
    }
}
