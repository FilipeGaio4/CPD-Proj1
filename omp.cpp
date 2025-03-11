#include <cstdlib>
#include <iomanip>
#include <iostream>
#include <omp.h>
#include <papi.h>
#include <stdio.h>
#include <time.h>

using namespace std;

#define SYSTEMTIME clock_t

double OnMult(int m_ar, int m_br) {

  SYSTEMTIME Time1, Time2;
  double ompTime1, ompTime2;
  char st[100];
  double temp;
  int i, j, k;

  double *pha, *phb, *phc;

  pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
  phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
  phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

  for (i = 0; i < m_ar; i++)
    for (j = 0; j < m_ar; j++)
      pha[i * m_ar + j] = (double)1.0;

  for (i = 0; i < m_br; i++)
    for (j = 0; j < m_br; j++)
      phb[i * m_br + j] = (double)(i + 1);

  Time1 = clock();
  ompTime1 = omp_get_wtime();

// #pragma omp parallel for collapse(2)
#pragma omp parallel for
  for (i = 0; i < m_ar; i++) {
    for (j = 0; j < m_br; j++) {
      temp = 0;
      for (k = 0; k < m_ar; k++) {
        temp += pha[i * m_ar + k] * phb[k * m_br + j];
      }
      phc[i * m_ar + j] = temp;
    }
  }

  Time2 = clock();
  ompTime2 = omp_get_wtime();
  // sprintf(st, "Time: %3.3f seconds\n",
  //         (double)(Time2 - Time1) / CLOCKS_PER_SEC);
  // cout << st;
  // sprintf(st, "Real Time: %3.3f seconds\n", (double)(ompTime2 - ompTime1));
  // cout << st;
  //
  // // display 10 elements of the result matrix tto verify correctness
  // cout << "Result matrix: " << endl;
  // for (i = 0; i < 1; i++) {
  //   for (j = 0; j < min(10, m_br); j++)
  //     cout << phc[j] << " ";
  // }
  // cout << endl;
  //
  free(pha);
  free(phb);
  free(phc);
  return ompTime2 - ompTime1;
}

double OnMultLine(int m_ar, int m_br) {
  SYSTEMTIME Time1, Time2;
  double ompTime1, ompTime2;
  char st[100];
  int i, j, k;
  double *pha, *phb, *phc;

  pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
  phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
  phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

  for (i = 0; i < m_ar; i++)
    for (j = 0; j < m_ar; j++)
      pha[i * m_ar + j] = (double)1.0;

  for (i = 0; i < m_br; i++)
    for (j = 0; j < m_br; j++)
      phb[i * m_br + j] = (double)(i + 1);

  Time1 = clock();
  ompTime1 = omp_get_wtime();

// #pragma omp parallel for    // Este é pior
#pragma omp parallel for collapse(2)
  for (i = 0; i < m_ar; i++)
    for (k = 0; k < m_ar; k++)
      for (j = 0; j < m_br; j++)
        phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];

  Time2 = clock();
  ompTime2 = omp_get_wtime();
  // sprintf(st, "Time: %3.3f seconds\n",
  //         (double)(Time2 - Time1) / CLOCKS_PER_SEC);
  // cout << st;
  // sprintf(st, "Real Time: %3.3f seconds\n", (double)(ompTime2 - ompTime1));
  // cout << st;
  //
  // cout << "Result matrix: " << endl;
  // for (i = 0; i < 1; i++) {
  //   for (j = 0; j < min(10, m_br); j++)
  //     cout << phc[j] << " ";
  // }
  cout << endl;

  free(pha);
  free(phb);
  free(phc);
  return ompTime2 - ompTime1;
}

double OnMultBlock(int m_ar, int m_br, int bkSize) {
  SYSTEMTIME Time1, Time2;
  double ompTime1, ompTime2;
  char st[100];
  int i, j, k, i1, j1, k1;
  double *pha, *phb, *phc;

  pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
  phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
  phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

  for (i = 0; i < m_ar; i++)
    for (j = 0; j < m_ar; j++)
      pha[i * m_ar + j] = (double)1.0;

  for (i = 0; i < m_br; i++)
    for (j = 0; j < m_br; j++)
      phb[i * m_br + j] = (double)(i + 1);

  Time1 = clock();
  ompTime1 = omp_get_wtime();

  int tid;
#pragma omp parallel
  {
#pragma omp for
    for (i = 0; i < m_ar; i += bkSize)     // Iterate over block
      for (j = 0; j < m_br; j += bkSize)   // Iterate over block
        for (k = 0; k < m_ar; k += bkSize) // Iterate over block
          for (i1 = i; i1 < i + bkSize && i1 < m_ar;
               i1++) // line mult algorithm i -> k -> j
            for (k1 = k; k1 < k + bkSize && k1 < m_ar; k1++)
              for (j1 = j; j1 < j + bkSize && j1 < m_br; j1++)
                phc[i1 * m_ar + j1] +=
                    pha[i1 * m_ar + k1] * phb[k1 * m_br + j1];
  }

  Time2 = clock();
  ompTime2 = omp_get_wtime();
  // sprintf(st, "Time: %3.3f seconds\n",
  //         (double)(Time2 - Time1) / CLOCKS_PER_SEC);
  // cout << st;
  // sprintf(st, "Real Time: %3.3f seconds\n", (double)(ompTime2 - ompTime1));
  // cout << st;

  // cout << "Result matrix: " << endl;
  for (i = 0; i < 1; i++) {
    for (j = 0; j < min(10, m_br); j++)
      cout << phc[j] << " ";
  }
  cout << endl;

  free(pha);
  free(phb);
  free(phc);
  return ompTime2 - ompTime1;
}

void handle_error(int retval) {
  printf("PAPI error %d: %s\n", retval, PAPI_strerror(retval));
  exit(1);
}

void init_papi() {
  int retval = PAPI_library_init(PAPI_VER_CURRENT);
  if (retval != PAPI_VER_CURRENT && retval < 0) {
    printf("PAPI library version mismatch!\n");
    exit(1);
  }
  if (retval < 0)
    handle_error(retval);

  std::cout << "PAPI Version Number: MAJOR: " << PAPI_VERSION_MAJOR(retval)
            << " MINOR: " << PAPI_VERSION_MINOR(retval)
            << " REVISION: " << PAPI_VERSION_REVISION(retval) << "\n";
}

int main(int argc, char *argv[]) {
  int op, lin, col, blockSize;

  int EventSet = PAPI_NULL;
  long long values[2];
  int ret;

  ret = PAPI_library_init(PAPI_VER_CURRENT);
  if (ret != PAPI_VER_CURRENT)
    std::cout << "FAIL" << endl;

  ret = PAPI_create_eventset(&EventSet);
  if (ret != PAPI_OK)
    cout << "ERROR: create eventset" << endl;

  ret = PAPI_add_event(EventSet, PAPI_L1_DCM);
  if (ret != PAPI_OK)
    cout << "ERROR: PAPI_L1_DCM" << endl;

  ret = PAPI_add_event(EventSet, PAPI_L2_DCM);
  if (ret != PAPI_OK)
    cout << "ERROR: PAPI_L2_DCM" << endl;

  if (argc < 3) {
    cout << "Usage: ./program <operation> <matrix_size> [block_size]" << endl;
    cout << "Operations: 1 = Multiplication, 2 = Line Multiplication, 3 = "
            "Block Multiplication"
         << endl;
    return 1;
  }

  op = atoi(argv[1]);  // Convert operation selection
  lin = atoi(argv[2]); // Convert matrix size
  col = lin;           // Square matrix

  double time;
  if (op == 3 && argc < 4) {
    cout << "Error: Block multiplication requires a block size parameter."
         << endl;
    return 1;
  } else if (op == 3) {
    blockSize = atoi(argv[3]);
  }
  ret = PAPI_start(EventSet);
  if (ret != PAPI_OK)
    cout << "ERROR: Start PAPI" << endl;

  switch (op) {
  case 1:
    time = OnMult(lin, col);
    break;
  case 2:
    time = OnMultLine(lin, col);
    break;
  case 3:
    time = OnMultBlock(lin, col, blockSize);
    break;
  default:
    cout << "Invalid operation selected!" << endl;
    return 1;
  }

  ret = PAPI_stop(EventSet, values);
  if (ret != PAPI_OK)
    cout << "ERROR: Stop PAPI" << endl;
  // printf("L1 DCM: %lld \n", values[0]);
  // printf("L2 DCM: %lld \n", values[1]);

  ret = PAPI_reset(EventSet);
  if (ret != PAPI_OK)
    std::cout << "FAIL reset" << endl;

  ret = PAPI_remove_event(EventSet, PAPI_L1_DCM);
  if (ret != PAPI_OK)
    std::cout << "FAIL remove event" << endl;

  ret = PAPI_remove_event(EventSet, PAPI_L2_DCM);
  if (ret != PAPI_OK)
    std::cout << "FAIL remove event" << endl;

  ret = PAPI_destroy_eventset(&EventSet);
  if (ret != PAPI_OK)
    std::cout << "FAIL destroy" << endl;

  cout << time << " " << values[0] << " "<< values[1];
}
