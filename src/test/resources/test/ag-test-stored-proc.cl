;; See the file LICENSE for the full license governing this code.

(define-condition my-stored-proc-error (simple-error) ())

(defun my-stored-proc-error (fmt &rest args)
  (error (make-condition 'my-stored-proc-error
                         :format-control fmt
                         :format-arguments args)))

(def-stored-proc add-two-vec-strings (&whole arg-vec)
  ;; takes two values and adds them
  (db.agraph::log-info :add-two-vec-strings "arg vec is ~s" arg-vec)
  (if (not (eql 2 (length arg-vec)))
      (my-stored-proc-error "wrong number of args")
      (write-to-string
       (+ (or (parse-integer (aref arg-vec 0)) 0)
          (or (parse-integer (aref arg-vec 1)) 0)))))

(def-stored-proc add-two-vec-ints (&whole arg-vec)
  ;; takes two values and adds them
  (db.agraph::log-info :add-two-vec-ints "arg vec is ~s" arg-vec)
  (if (not (eql 2 (length arg-vec)))
      "wrong number of args"
       (+ (aref arg-vec 0)
          (aref arg-vec 1))))

(def-stored-proc add-two-strings (a b)
  ;; takes two values and adds them
  (write-to-string
   (+ (or (parse-integer a) 0)
      (or (parse-integer b) 0))))

(def-stored-proc add-two-ints (a b)
  ;; takes two values and adds them
  (+ a b))

(def-stored-proc best-be-nil (a)
  (if a (my-stored-proc-error "I expected a nil, but got: ~a" a)))

(def-stored-proc identity (a)
  a)

(defun make-all-types ()
  (vector 123
          0
          -123
          "abc"
          nil
          (make-array 4 :initial-element 9)
          (list 123 0 -123 "abc")
          (make-array 8 :element-type '(unsigned-byte 8)
                      :initial-contents '(0 1 2 3 4 5 6 7))
          ))

(def-stored-proc return-all-types ()
  (make-all-types))

(def-stored-proc check-all-types (in)
  (labels ((check (expected actual)
             (cond ((vectorp expected)
                    (map 'list (lambda (e a) (check e a)) expected actual))
                   ((not (equal expected actual))
                    (my-stored-proc-error "expected ~a, actual ~a" expected actual)))))
    (check (make-all-types) in))
  in)

(def-stored-proc add-a-triple-int (i)
  (add-triple (resource "http://test.com/add-a-triple-int") (resource "http://test.com/p") (value->upi i :int)))

(def-stored-proc get-a-triple-int (i)
  (let ((tr (get-triple :s (resource "http://test.com/add-a-triple-int")
                        :p (resource "http://test.com/p")
                        :o (value->upi i :int))))
    (when tr
      (with-output-to-string (out) (print-triple tr :stream out :format :long)))))

;; (def-stored-proc add-a-triple (s p o)
;;   (add-triple s p o))
