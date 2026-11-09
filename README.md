# ChessBot Lab v0.5 — experimental move candidates

Captures screenshots with MediaProjection, supports manual board calibration and ranks changed squares. A new experimental move tracker starts from the standard chess opening position and tries to match **exactly two changed squares** to one piece move. It does not implement full legal chess rules (castling, en passant, check validation, promotions), does not reconstruct missed moves, and may misinterpret highlights. It is not reliable for autonomous play.

For a meaningful test: reset the tracker, capture a fresh initial board, make exactly **one** move, capture again, then press **Tek hamleyi tahmin et**. Do not skip turns. Reset on uncertain output. No accessibility service, engine, or automated taps.
