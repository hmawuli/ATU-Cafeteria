// infinityfree_challenge_solver.dart
//
// InfinityFree's free tier runs a "Browser Security System": before your files
// are touched it answers requests with a small HTML page whose JavaScript
// decrypts an AES token and sets a `__test` cookie. Real browsers run the JS
// invisibly; native apps (Flutter) cannot. This class computes the exact same
// cookie value in Dart (a faithful port of the server's own slowAES decrypt,
// CBC mode, AES-128), then retries the request with that cookie.
//
// Pure Dart: no Flutter / package dependencies.
import 'dart:typed_data';

const List<int> _sbox = [99,124,119,123,242,107,111,197,48,1,103,43,254,215,171,118,202,130,201,125,250,89,71,240,173,212,162,175,156,164,114,192,183,253,147,38,54,63,247,204,52,165,229,241,113,216,49,21,4,199,35,195,24,150,5,154,7,18,128,226,235,39,178,117,9,131,44,26,27,110,90,160,82,59,214,179,41,227,47,132,83,209,0,237,32,252,177,91,106,203,190,57,74,76,88,207,208,239,170,251,67,77,51,133,69,249,2,127,80,60,159,168,81,163,64,143,146,157,56,245,188,182,218,33,16,255,243,210,205,12,19,236,95,151,68,23,196,167,126,61,100,93,25,115,96,129,79,220,34,42,144,136,70,238,184,20,222,94,11,219,224,50,58,10,73,6,36,92,194,211,172,98,145,149,228,121,231,200,55,109,141,213,78,169,108,86,244,234,101,122,174,8,186,120,37,46,28,166,180,198,232,221,116,31,75,189,139,138,112,62,181,102,72,3,246,14,97,53,87,185,134,193,29,158,225,248,152,17,105,217,142,148,155,30,135,233,206,85,40,223,140,161,137,13,191,230,66,104,65,153,45,15,176,84,187,22];

const List<int> _rsbox = [82,9,106,213,48,54,165,56,191,64,163,158,129,243,215,251,124,227,57,130,155,47,255,135,52,142,67,68,196,222,233,203,84,123,148,50,166,194,35,61,238,76,149,11,66,250,195,78,8,46,161,102,40,217,36,178,118,91,162,73,109,139,209,37,114,248,246,100,134,104,152,22,212,164,92,204,93,101,182,146,108,112,72,80,253,237,185,218,94,21,70,87,167,141,157,132,144,216,171,0,140,188,211,10,247,228,88,5,184,179,69,6,208,44,30,143,202,63,15,2,193,175,189,3,1,19,138,107,58,145,17,65,79,103,220,234,151,242,207,206,240,180,230,115,150,172,116,34,231,173,53,133,226,249,55,232,28,117,223,110,71,241,26,113,29,41,197,137,111,183,98,14,170,24,190,27,252,86,62,75,198,210,121,32,154,219,192,254,120,205,90,244,31,221,168,51,136,7,199,49,177,18,16,89,39,128,236,95,96,81,127,169,25,181,74,13,45,229,122,159,147,201,156,239,160,224,59,77,174,42,245,176,200,235,187,60,131,83,153,97,23,43,4,126,186,119,214,38,225,105,20,99,85,33,12,125];

class InfinityFreeChallengeSolver {
  InfinityFreeChallengeSolver._();

  static String? _cookie;
  static DateTime? _issuedAt;
  static const Duration _lifetime = Duration(hours: 6);

  /// The currently valid cookie, or null if absent/expired (server issues
  /// cookies with a 6-hour max-age).
  static String? get cookies {
    final c = _cookie;
    final at = _issuedAt;
    if (c == null || at == null) return null;
    if (DateTime.now().difference(at) > _lifetime) return null;
    return c;
  }

  /// True when [body] looks like InfinityFree's JS challenge page.
  static bool isChallenge(String contentType, String body) {
    return (contentType.contains('text/html') || contentType.isEmpty) &&
        body.contains('slowAES') &&
        body.contains('__test');
  }

  /// Parse the challenge page and return the matching `__test` value.
  static String? solveFromHtml(String html) {
    final m = RegExp(
      r'var a=toNumbers\("([0-9a-f]+)"\),b=toNumbers\("([0-9a-f]+)"\),c=toNumbers\("([0-9a-f]+)"\)',
    ).firstMatch(html);
    if (m == null) return null;
    final key = _hex(m.group(1)!);
    final iv = _hex(m.group(2)!);
    final cipher = _hex(m.group(3)!);
    final plain = _aes128CbcDecrypt(cipher, key, iv);
    final cookie = _toHex(plain);
    _cookie = cookie;
    _issuedAt = DateTime.now();
    return cookie;
  }

  // ---- hex helpers ---------------------------------------------------------

  static List<int> _hex(String s) {
    final out = Uint8List(s.length ~/ 2);
    for (var i = 0; i < out.length; i++) {
      out[i] = int.parse(s.substring(i * 2, i * 2 + 2), radix: 16);
    }
    return out;
  }

  static String _toHex(List<int> bytes) {
    final sb = StringBuffer();
    for (final v in bytes) {
      sb.write(v.toRadixString(16).padLeft(2, '0'));
    }
    return sb.toString();
  }

  // ---- AES-128-CBC (single block) decrypt, ported from the server's slowAES --

  static List<int> _aes128CbcDecrypt(List<int> cipher, List<int> key, List<int> iv) {
    final plain = _aesDecryptBlock(cipher, key, key.length);
    for (var i = 0; i < 16; i++) {
      plain[i] ^= iv[i];
    }
    return plain;
  }

  static int _numberOfRounds(int keyLen) {
    switch (keyLen) {
      case 24:
        return 12;
      case 32:
        return 14;
      default:
        return 10;
    }
  }

  static List<int> _rotate(List<int> input) {
    final t = input[0];
    for (var r = 0; r < 3; r++) {
      input[r] = input[r + 1];
    }
    input[3] = t;
    return input;
  }

  static List<int> _core(List<int> input, int t) {
    input = _rotate(input);
    for (var r = 0; r < 4; ++r) {
      input[r] = _sbox[input[r]];
    }
    input[0] = input[0] ^ _rcon(t);
    return input;
  }

  static int _rcon(int i) => switch (i) {
        1 => 1,
        2 => 2,
        3 => 4,
        4 => 8,
        5 => 16,
        6 => 32,
        7 => 64,
        8 => 128,
        9 => 27,
        10 => 54,
        11 => 108,
        12 => 216,
        13 => 171,
        14 => 77,
        15 => 154,
        _ => 47,
      };

  static List<int> _expandKey(List<int> key, int keyLen) {
    final r = 16 * (_numberOfRounds(keyLen) + 1);
    final e = List<int>.filled(r, 0);
    for (var h = 0; h < keyLen; h++) {
      e[h] = key[h];
    }
    var o = keyLen, n = 1;
    final s = List<int>.filled(4, 0);
    while (o < r) {
      for (var u = 0; u < 4; u++) {
        s[u] = e[o - 4 + u];
      }
      if (o % keyLen == 0) _core(s, n++);
      if (keyLen == 32 && o % keyLen == 16) {
        for (var f = 0; f < 4; f++) {
          s[f] = _sbox[s[f]];
        }
      }
      for (var l = 0; l < 4; l++) {
        e[o] = e[o - keyLen] ^ s[l];
        o++;
      }
    }
    return e;
  }

  static List<int> _addRoundKey(List<int> state, List<int> roundKey) {
    for (var r = 0; r < 16; r++) {
      state[r] = state[r] ^ roundKey[r];
    }
    return state;
  }

  static List<int> _createRoundKey(List<int> expanded, int offset) {
    final r = List<int>.filled(16, 0);
    for (var o = 0; o < 4; o++) {
      for (var n = 0; n < 4; n++) {
        r[4 * n + o] = expanded[offset + 4 * o + n];
      }
    }
    return r;
  }

  static void _subBytes(List<int> state, bool invert) {
    for (var r = 0; r < 16; r++) {
      state[r] = invert ? _rsbox[state[r]] : _sbox[state[r]];
    }
  }

  static void _shiftRow(List<int> state, int start, int shift, bool invert) {
    for (var n = 0; n < shift; n++) {
      if (invert) {
        final s = state[start + 3];
        for (var e = 3; e > 0; e--) {
          state[start + e] = state[start + e - 1];
        }
        state[start] = s;
      } else {
        final s = state[start];
        for (var e = 0; e < 3; e++) {
          state[start + e] = state[start + e + 1];
        }
        state[start + 3] = s;
      }
    }
  }

  static void _shiftRows(List<int> state, bool invert) {
    for (var r = 0; r < 4; r++) {
      _shiftRow(state, 4 * r, r, invert);
    }
  }

  static int _galois(int a, int b) {
    var r = 0;
    var aa = a, bb = b;
    for (var o = 0; o < 8; o++) {
      if ((bb & 1) == 1) r ^= aa;
      if (r > 256) r ^= 256;
      final n = 128 & aa;
      aa = aa << 1;
      if (aa > 256) aa ^= 256;
      if (n == 128) aa ^= 27;
      if (aa > 256) aa ^= 256;
      bb = bb >> 1;
      if (bb > 256) bb ^= 256;
    }
    return r;
  }

  static void _mixColumn(List<int> i, bool invert) {
    final r = invert ? <int>[14, 9, 13, 11] : <int>[2, 1, 1, 3];
    final o = <int>[i[0], i[1], i[2], i[3]];
    i[0] = _galois(o[0], r[0]) ^ _galois(o[3], r[1]) ^ _galois(o[2], r[2]) ^ _galois(o[1], r[3]);
    i[1] = _galois(o[1], r[0]) ^ _galois(o[0], r[1]) ^ _galois(o[3], r[2]) ^ _galois(o[2], r[3]);
    i[2] = _galois(o[2], r[0]) ^ _galois(o[1], r[1]) ^ _galois(o[0], r[2]) ^ _galois(o[3], r[3]);
    i[3] = _galois(o[3], r[0]) ^ _galois(o[2], r[1]) ^ _galois(o[1], r[2]) ^ _galois(o[0], r[3]);
  }

  static void _mixColumns(List<int> state, bool invert) {
    for (var col = 0; col < 4; col++) {
      final r = <int>[
        state[col],
        state[4 + col],
        state[8 + col],
        state[12 + col],
      ];
      _mixColumn(r, invert);
      for (var row = 0; row < 4; row++) {
        state[4 * row + col] = r[row];
      }
    }
  }

  static void _invRound(List<int> state, List<int> roundKey) {
    _shiftRows(state, true);
    _subBytes(state, true);
    _addRoundKey(state, roundKey);
    _mixColumns(state, true);
  }

  static List<int> _invMain(List<int> state, List<int> expanded, int rounds) {
    _addRoundKey(state, _createRoundKey(expanded, 16 * rounds));
    for (var o = rounds - 1; o > 0; o--) {
      _invRound(state, _createRoundKey(expanded, 16 * o));
    }
    _shiftRows(state, true);
    _subBytes(state, true);
    _addRoundKey(state, _createRoundKey(expanded, 0));
    return state;
  }

  static List<int> _aesDecryptBlock(List<int> block, List<int> key, int keyLen) {
    final state = List<int>.filled(16, 0);
    final out = List<int>.filled(16, 0);
    for (var e = 0; e < 4; e++) {
      for (var a = 0; a < 4; a++) {
        state[e + 4 * a] = block[4 * e + a];
      }
    }
    final expanded = _expandKey(key, keyLen);
    final result = _invMain(state, expanded, _numberOfRounds(keyLen));
    for (var h = 0; h < 4; h++) {
      for (var u = 0; u < 4; u++) {
        out[4 * h + u] = result[h + 4 * u];
      }
    }
    return out;
  }
}