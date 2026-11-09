# ChessBot Lab v0.5 — experimental move candidates

Captures screenshots with MediaProjection, supports manual board calibration and ranks changed squares. A new experimental move tracker starts from the standard chess opening position and tries to match **exactly two changed squares** to one piece move. It does not implement full legal chess rules (castling, en passant, check validation, promotions), does not reconstruct missed moves, and may misinterpret highlights. It is not reliable for autonomous play.

For a meaningful test: reset the tracker, capture a fresh initial board, make exactly **one** move, capture again, then press **Tek hamleyi tahmin et**. Do not skip turns. Reset on uncertain output. No accessibility service, engine, or automated taps.

## v0.5.1 history sync

Enter **every prior move in chronological UCI form** (e.g. `g1f3 g8f6 d2d3`) and press **Hamle geçmişini eşitle** before comparing a screenshot pair containing the next single move. The tracker assumes white at bottom and starts from the standard chess position. It has no complete legality engine, promotion/castling/en passant support, or robust handling for screenshots with multiple moves. Incorrect history must be reset rather than guessed.

## v0.6 experimental two-move detection

The **1 veya 2 hamleyi analiz et** button evaluates changed squares against the internally tracked position. It can recognize some sequences of two non-capturing moves (e.g. g1f3 d7d6) without requiring separate screenshots between the moves. It refuses ambiguous candidate sets. It still requires synchronization with the actual game position and cannot recover arbitrary missed moves. Castling, en passant, promotion, captures in two-move mode, and full legal-check validation are unsupported. Do not use for automatic play.
