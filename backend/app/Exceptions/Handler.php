<?php

namespace App\Exceptions;

use Illuminate\Auth\Access\AuthorizationException;
use Illuminate\Auth\AuthenticationException;
use Illuminate\Database\Eloquent\ModelNotFoundException;
use Illuminate\Database\QueryException;
use Illuminate\Foundation\Exceptions\Handler as ExceptionHandler;
use Illuminate\Support\Facades\Log;
use Illuminate\Validation\ValidationException;
use Psr\Log\LogLevel;
use Symfony\Component\HttpKernel\Exception\HttpExceptionInterface;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;
use Throwable;

class Handler extends ExceptionHandler
{
    /**
     * A list of exception types with their corresponding custom log levels.
     *
     * @var array<class-string<Throwable>, LogLevel::*>
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
        'pin',
    ];

    /**
     * Register the exception handling callbacks for the application.
     */
    public function register(): void
    {
        $this->reportable(function (Throwable $e) {
            if ($this->shouldReport($e)) {
                Log::error(sprintf(
                    'Application Exception [%s]: %s in %s:%d',
                    get_class($e),
                    $e->getMessage(),
                    $e->getFile(),
                    $e->getLine()
                ));
            }
        });

        // 1. Unified JSON rendering for API requests
        $this->renderable(function (Throwable $e, $request) {
            if ($request->is('api/*') || $request->expectsJson() || $request->ajax()) {
                return $this->handleApiJsonResponse($e, $request);
            }
        });
    }

    /**
     * Build unified JSON response for API exceptions.
     */
    protected function handleApiJsonResponse(Throwable $e, $request)
    {
        $statusCode = 500;
        $errorCode = 'INTERNAL_SERVER_ERROR';
        $message = 'An unexpected error occurred on the server.';
        $errors = null;

        if ($e instanceof ValidationException) {
            $statusCode = 422;
            $errorCode = 'VALIDATION_FAILED';
            $message = 'The provided request data failed validation.';
            $errors = $e->errors();
        } elseif ($e instanceof AuthenticationException) {
            $statusCode = 401;
            $errorCode = 'UNAUTHENTICATED';
            $message = 'Authentication credentials missing or invalid.';
        } elseif ($e instanceof AuthorizationException) {
            $statusCode = 403;
            $errorCode = 'FORBIDDEN';
            $message = 'You are not authorized to perform this action.';
        } elseif ($e instanceof ModelNotFoundException || $e instanceof NotFoundHttpException) {
            $statusCode = 404;
            $errorCode = 'RESOURCE_NOT_FOUND';
            $message = 'The requested resource or endpoint was not found.';
        } elseif ($e instanceof QueryException) {
            $statusCode = 500;
            $errorCode = 'DATABASE_QUERY_ERROR';
            $message = 'A database operation failed. Please verify entity parameters.';
            Log::error('API Query Exception: '.$e->getMessage(), ['sql' => $e->getSql()]);
        } elseif ($e instanceof HttpExceptionInterface) {
            $statusCode = $e->getStatusCode();
            $errorCode = 'HTTP_EXCEPTION_'.$statusCode;
            $message = $e->getMessage() ?: 'HTTP Request Error';
        } else {
            if (config('app.debug')) {
                $message = $e->getMessage();
            }
        }

        return response()->json([
            'success' => false,
            'message' => $message,
            'error_code' => $errorCode,
            'errors' => $errors,
            'status_code' => $statusCode,
            'debug' => config('app.debug') ? [
                'exception' => get_class($e),
                'file' => $e->getFile(),
                'line' => $e->getLine(),
                'trace' => array_slice(explode("\n", $e->getTraceAsString()), 0, 5),
            ] : null,
        ], $statusCode);
    }
}
