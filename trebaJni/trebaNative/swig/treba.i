 %module treba
 %{
 /* Includes the header in the wrapper code */
 #include "treba.h"
 %}
 
 /* Parse the header file to generate wrappers */
 %include "treba.h"
